import Combine
import Core
import DataBet
import Foundation
@testable import Bet

/// A clock that moves only when a test advances it, so debounce timing is exact.
final class ManualClock: Clock, @unchecked Sendable {
    struct Instant: InstantProtocol {
        let offset: Duration

        func advanced(by duration: Duration) -> Instant {
            Instant(offset: offset + duration)
        }

        func duration(to other: Instant) -> Duration {
            other.offset - offset
        }

        static func < (lhs: Instant, rhs: Instant) -> Bool {
            lhs.offset < rhs.offset
        }
    }

    private struct Sleeper {
        let deadline: Instant
        let continuation: CheckedContinuation<Void, Error>
    }

    private let lock = NSLock()
    private var current = Instant(offset: .zero)
    private var sleepers: [UUID: Sleeper] = [:]

    var now: Instant { lock.withLock { current } }
    var minimumResolution: Duration { .zero }

    /// Sleeps that have neither ended nor been cancelled.
    var sleeperCount: Int { lock.withLock { sleepers.count } }

    func sleep(until deadline: Instant, tolerance: Duration?) async throws {
        let id = UUID()
        try await withTaskCancellationHandler {
            try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
                let outcome: Result<Void, Error>? = lock.withLock {
                    if Task.isCancelled { return .failure(CancellationError()) }
                    if deadline <= current { return .success(()) }
                    sleepers[id] = Sleeper(deadline: deadline, continuation: continuation)
                    return nil
                }
                if let outcome { continuation.resume(with: outcome) }
            }
        } onCancel: {
            let sleeper = lock.withLock { sleepers.removeValue(forKey: id) }
            sleeper?.continuation.resume(throwing: CancellationError())
        }
    }

    /// Moves time forward and wakes every sleep whose deadline has passed.
    func advance(by duration: Duration) {
        let due: [Sleeper] = lock.withLock {
            current = current.advanced(by: duration)
            let dueIds = sleepers.filter { $0.value.deadline <= current }.map(\.key)
            return dueIds.compactMap { sleepers.removeValue(forKey: $0) }
        }
        due.forEach { $0.continuation.resume() }
    }

    /// Waits until `count` sleeps are pending.
    /// Budgets polls, not wall time: a busy CI main actor can stall this task far past any deadline.
    func waitForSleepers(_ count: Int, polls: Int = 500) async throws {
        var remaining = polls
        while sleeperCount != count {
            guard remaining > 0 else { throw PendingBetTimeout() }
            remaining -= 1
            try await Task.sleep(for: .milliseconds(10))
        }
    }
}

/// Serves pending-feed pages by cursor and holds each submitted prediction until the test
/// completes it, so the saving state can be observed before the result arrives.
@MainActor
final class ScriptedPendingBetRepository: PoolGamblerBetRepository {
    /// The pages in feed order; a request with a cursor gets the page after the one that issued it.
    var pages: [CursorPage<PoolGamblerBet>]
    private(set) var submissions: [Bet] = []
    private var pending: CheckedContinuation<Result<PoolGamblerBet, Error>, Never>?
    private var consumed = 0

    init(pages: [CursorPage<PoolGamblerBet>] = []) {
        self.pages = pages
    }

    var submissionCount: Int { submissions.count }

    /// Waits for the next submission that has not been returned yet and is awaiting a result.
    /// Budgets polls, not wall time: a busy CI main actor can stall this task far past any deadline.
    func nextSubmission(polls: Int = 500) async throws -> Bet {
        var remaining = polls
        while pending == nil || submissions.count <= consumed {
            guard remaining > 0 else { throw PendingBetTimeout() }
            remaining -= 1
            try await Task.sleep(for: .milliseconds(10))
        }
        consumed += 1
        return submissions[consumed - 1]
    }

    /// Answers the held submission with a success that stores its scores.
    func completeSaving(_ submitted: Bet, as model: PoolGamblerBetModel) {
        let saved = model.copy {
            $0.betScore = TeamScore(
                homeTeamValue: submitted.homeTeamBet.value,
                awayTeamValue: submitted.awayTeamBet.value
            )
        }
        complete(with: .success(PendingBetFixtures.bet(saved)))
    }

    func complete(with result: Result<PoolGamblerBet, Error>) {
        let continuation = pending
        pending = nil
        continuation?.resume(returning: result)
    }

    nonisolated func bet(bet: Bet) async -> Result<PoolGamblerBet, Error> {
        await withCheckedContinuation { continuation in
            Task { @MainActor in
                self.submissions.append(bet)
                self.pending = continuation
            }
        }
    }

    /// When set, a request with a cursor waits until `releaseHeldPage()` instead of answering at
    /// once, so a test can observe the list while a later page is loading.
    var holdsPagesWithCursor = false
    private var heldPage: CheckedContinuation<Void, Never>?

    var isHoldingPage: Bool { heldPage != nil }

    func releaseHeldPage() {
        let continuation = heldPage
        heldPage = nil
        continuation?.resume()
    }

    nonisolated func getPendingPoolGamblerBets(
        poolId: String, gamblerId: String, next: String?, searchText: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> {
        if next != nil, await MainActor.run(body: { self.holdsPagesWithCursor }) {
            await withCheckedContinuation { continuation in
                Task { @MainActor in self.heldPage = continuation }
            }
        }
        return await MainActor.run { self.page(after: next) }
    }

    private func page(after next: String?) -> Result<CursorPage<PoolGamblerBet>, Error> {
        guard let next else { return .success(pages.first ?? CursorPage(items: [], next: nil)) }
        let index = pages.firstIndex { $0.next == next }.map { $0 + 1 } ?? pages.count
        return index < pages.count ? .success(pages[index]) : .success(CursorPage(items: [], next: nil))
    }

    nonisolated func getFinishedPoolGamblerBets(
        poolId: String, gamblerId: String, next: String?
    ) async -> Result<CursorPage<KickoffGroup>, Error> { .failure(Unused()) }

    nonisolated func getLivePoolGamblerBets(
        poolId: String, gamblerId: String, next: String?, searchText: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> { .failure(Unused()) }

    nonisolated func getRankedPoolMatchGamblerBets(
        poolId: String, matchId: String, next: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> { .failure(Unused()) }

    nonisolated func getPoolGamblerBet(
        poolId: String, gamblerId: String, matchId: String
    ) async -> Result<PoolGamblerBet, Error> { .failure(Unused()) }

    nonisolated func getGamblerBetsTimeline(
        poolId: String, gamblerId: String, next: String?
    ) async -> Result<CursorPage<KickoffGroup>, Error> { .failure(Unused()) }

    private struct Unused: Error {}
}

/// Records the outcomes a row reports.
@MainActor
final class PendingBetOutcomeLog: PendingBetRowOutcomes {
    enum Event: Equatable {
        case saved(String)
        case failed(String)
        case failureCleared(String)
        case locked(String)
    }

    private(set) var events: [Event] = []

    func saved(_ matchId: String) { events.append(.saved(matchId)) }
    func failed(_ matchId: String) { events.append(.failed(matchId)) }
    func failureCleared(_ matchId: String) { events.append(.failureCleared(matchId)) }
    func locked(_ matchId: String) { events.append(.locked(matchId)) }
}

enum PendingBetFixtures {
    /// Friday, 12 June 2026, 17:00 UTC, plus `days`.
    static func kickoff(days: Int = 0) -> Date {
        Date(timeIntervalSince1970: 1_781_283_600 + Double(days) * 86_400)
    }

    /// An open, uncomputed match.
    static func match(
        _ matchId: String,
        betScore: TeamScore<Int>? = nil,
        isLocked: Bool = false,
        kickoffDay: Int = 0
    ) -> PoolGamblerBetModel {
        poolGamblerBetDummyModel().copy {
            $0.matchId = matchId
            $0.betScore = betScore
            $0.matchScore = nil
            $0.score = nil
            $0.isComputed = false
            $0.matchDateTime = kickoff(days: kickoffDay)
            if isLocked { $0.lock() }
        }
    }

    static func page(_ matches: [PoolGamblerBetModel], next: String? = nil) -> CursorPage<PoolGamblerBet> {
        CursorPage(items: matches.map(bet), next: next)
    }

    /// `PoolGamblerBet`'s memberwise initializer is internal to `DataBet`; both types share their
    /// Codable shape.
    static func bet(_ model: PoolGamblerBetModel) -> PoolGamblerBet {
        try! JSONDecoder().decode(PoolGamblerBet.self, from: JSONEncoder().encode(model))
    }
}

struct PendingBetTimeout: Error {}

/// A row's draft in a comparable form; the draft itself is a tuple.
struct DraftScores: Equatable {
    let home: Int?
    let away: Int?

    init(_ home: Int?, _ away: Int?) {
        self.home = home
        self.away = away
    }
}

extension PendingBetRowHolder {
    var draftScores: DraftScores { DraftScores(draft.home, draft.away) }
}

/// Every state a row passes through, consecutive repeats collapsed. `objectWillChange` fires
/// before each change, so each sample is the state being left; the current state ends the list.
@MainActor
final class RowStateHistory {
    private let row: PendingBetRowHolder
    private var samples: [PendingBetRowHolder.RowState] = []
    private var subscription: AnyCancellable?

    init(_ row: PendingBetRowHolder) {
        self.row = row
        subscription = row.objectWillChange.sink { [weak self] in
            guard let self else { return }
            self.samples.append(self.row.rowState)
        }
    }

    var states: [PendingBetRowHolder.RowState] {
        (samples + [row.rowState]).reduce(into: []) { states, state in
            if states.last != state { states.append(state) }
        }
    }
}

/// Polls `condition` on the main actor.
/// Budgets polls, not wall time: a busy CI main actor can stall this task far past any deadline.
@MainActor
func eventually(polls: Int = 500, _ condition: @MainActor () -> Bool) async -> Bool {
    var remaining = polls
    while !condition() {
        guard remaining > 0 else { return false }
        remaining -= 1
        try? await Task.sleep(for: .milliseconds(10))
    }
    return true
}

/// Lets queued main-actor work run, for checks that something did not happen.
@MainActor
func settle() async {
    for _ in 0..<20 { await Task.yield() }
    try? await Task.sleep(for: .milliseconds(30))
}

/// What SwiftUI would render after each change: every `objectWillChange` schedules one sample for
/// the next main-queue pass, as a view update would, so changes made together in one main-actor
/// turn yield identical samples.
@MainActor
final class RenderedRowLog {
    struct Frame: Equatable {
        let state: PendingBetRowHolder.RowState
        let draft: DraftScores
    }

    private let row: PendingBetRowHolder
    private(set) var frames: [Frame] = []
    private var subscription: AnyCancellable?

    init(_ row: PendingBetRowHolder) {
        self.row = row
        subscription = row.objectWillChange.sink { [weak self] in
            DispatchQueue.main.async {
                guard let self else { return }
                self.frames.append(Frame(state: self.row.rowState, draft: self.row.draftScores))
            }
        }
    }

    /// The distinct frames in order, consecutive repeats collapsed.
    var distinctFrames: [Frame] {
        frames.reduce(into: []) { result, frame in
            if result.last != frame { result.append(frame) }
        }
    }
}
