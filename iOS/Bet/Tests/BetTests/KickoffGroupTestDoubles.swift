import Core
import DataBet
import Foundation
@testable import Bet

/// Grouped-feed fixtures. `KickoffGroup`'s memberwise initializer is internal to `DataBet`; the
/// fixtures go through its public Codable shape.
enum GroupFixtures {
    /// Friday, 12 June 2026, 17:00 UTC, plus `hours`.
    static func kickoff(hours: Int = 0) -> Date {
        Date(timeIntervalSince1970: 1_781_283_600 + Double(hours) * 3_600)
    }

    /// A group whose rows award 5 points each once computed. `matchIds` default to
    /// `<prefix>-0`, `<prefix>-1`, ... for `rows` rows, the first `computedRows` of them computed.
    static func group(
        _ prefix: String,
        kickoff: Date = kickoff(),
        total: Int = 2,
        computed: Int = 2,
        rows: Int? = nil,
        computedRows: Int? = nil,
        movement: (before: Int, after: Int)? = (8, 5)
    ) -> KickoffGroup {
        let rowCount = rows ?? total
        let computedRowCount = computedRows ?? min(computed, rowCount)
        let fixture = Fixture(
            kickoffDateTime: kickoff,
            totalMatches: total,
            computedMatches: computed,
            movement: movement.map { Movement(beforePosition: $0.before, position: $0.after) },
            matches: (0..<rowCount).map { index in
                let isComputed = index < computedRowCount
                return bet(poolGamblerBetDummyModel().copy {
                    $0.matchId = "\(prefix)-\(index)"
                    $0.matchDateTime = kickoff
                    $0.isComputed = isComputed
                    $0.score = isComputed ? 5 : nil
                    $0.matchScore = isComputed ? $0.matchScore : nil
                })
            }
        )
        return try! JSONDecoder().decode(KickoffGroup.self, from: JSONEncoder().encode(fixture))
    }

    static func page(_ groups: [KickoffGroup], next: String? = nil) -> CursorPage<KickoffGroup> {
        CursorPage(items: groups, next: next)
    }

    /// `PoolGamblerBet`'s memberwise initializer is internal to `DataBet`; both types share their
    /// Codable shape.
    static func bet(_ model: PoolGamblerBetModel) -> PoolGamblerBet {
        try! JSONDecoder().decode(PoolGamblerBet.self, from: JSONEncoder().encode(model))
    }

    private struct Fixture: Encodable {
        let kickoffDateTime: Date
        let totalMatches: Int
        let computedMatches: Int
        let movement: Movement?
        let matches: [PoolGamblerBet]
    }

    private struct Movement: Encodable {
        let beforePosition: Int
        let position: Int
    }
}

struct GroupedFeedRequest: Equatable {
    let poolId: String
    let gamblerId: String
    let next: String?
}

/// Serves History and Timeline pages from a script. By default the first request (no cursor) gets
/// the first page and a cursor gets the page after the one that issued it; `responder` replaces
/// that rule, and `holdsRequests` keeps each request open until the test releases it, so responses
/// can arrive out of order. Pages and failures can change between requests.
@MainActor
final class ScriptedGroupedFeedRepository: PoolGamblerBetRepository {
    var pages: [CursorPage<KickoffGroup>]
    /// While set, every request fails with it.
    var failure: Error?
    var responder: ((String?) -> Result<CursorPage<KickoffGroup>, Error>)?
    var holdsRequests = false
    private(set) var requests: [GroupedFeedRequest] = []
    private var held: [CheckedContinuation<Void, Never>] = []

    init(pages: [CursorPage<KickoffGroup>] = [], failsWith failure: Error? = nil) {
        self.pages = pages
        self.failure = failure
    }

    var requestCount: Int { requests.count }

    /// Budgets polls, not wall time: a busy CI main actor can stall this task far past any deadline.
    func waitForHeldRequests(_ count: Int, polls: Int = 500) async throws {
        var remaining = polls
        while held.count < count {
            guard remaining > 0 else { throw FeedTimeout() }
            remaining -= 1
            try await Task.sleep(for: .milliseconds(10))
        }
    }

    /// Releases the held request at `index`, in request order among those still held.
    func releaseHeldRequest(at index: Int = 0) {
        held.remove(at: index).resume()
    }

    private func respond(poolId: String, gamblerId: String, next: String?) async -> Result<CursorPage<KickoffGroup>, Error> {
        requests.append(GroupedFeedRequest(poolId: poolId, gamblerId: gamblerId, next: next))
        if holdsRequests {
            await withCheckedContinuation { continuation in held.append(continuation) }
        }
        if let failure { return .failure(failure) }
        if let responder { return responder(next) }
        guard let next else { return .success(pages.first ?? CursorPage(items: [], next: nil)) }
        let index = pages.firstIndex { $0.next == next }.map { $0 + 1 } ?? pages.count
        return .success(index < pages.count ? pages[index] : CursorPage(items: [], next: nil))
    }

    nonisolated func getFinishedPoolGamblerBets(
        poolId: String, gamblerId: String, next: String?
    ) async -> Result<CursorPage<KickoffGroup>, Error> {
        await respond(poolId: poolId, gamblerId: gamblerId, next: next)
    }

    nonisolated func getGamblerBetsTimeline(
        poolId: String, gamblerId: String, next: String?
    ) async -> Result<CursorPage<KickoffGroup>, Error> {
        await respond(poolId: poolId, gamblerId: gamblerId, next: next)
    }

    nonisolated func getPoolGamblerBet(
        poolId: String, gamblerId: String, matchId: String
    ) async -> Result<PoolGamblerBet, Error> { .failure(Unused()) }

    nonisolated func getPendingPoolGamblerBets(
        poolId: String, gamblerId: String, next: String?, searchText: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> { .failure(Unused()) }

    nonisolated func getLivePoolGamblerBets(
        poolId: String, gamblerId: String, next: String?, searchText: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> { .failure(Unused()) }

    nonisolated func getRankedPoolMatchGamblerBets(
        poolId: String, matchId: String, next: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> { .failure(Unused()) }

    nonisolated func bet(bet: Bet) async -> Result<PoolGamblerBet, Error> { .failure(Unused()) }

    private struct Unused: Error {}
    struct FeedTimeout: Error {}
}
