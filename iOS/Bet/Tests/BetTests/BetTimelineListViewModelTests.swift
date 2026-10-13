import Core
import DataBet
import DataPool
import Foundation
import Testing
import UI
import ViewInspector
@testable import Bet

/// Timeline's rows and authoritative total, as explored on the iOS 18.1 simulator: rows load on
/// entry, pull to refresh reloads them, and appending follows the feed cursor. Failures,
/// out-of-order responses, uncomputed entries, and context changes cannot be staged with real
/// data, so these tests cover them.
@MainActor
struct BetTimelineListViewModelTests {
    @Test
    func rowsAndTotalAreRequestedForTheSelectedPoolAndGambler() async throws {
        let scores = TimelineGatedScoreRepository()
        let bets = ScriptedGroupedFeedRepository(pages: [timelinePage(count: 2, next: nil)])
        let viewModel = makeViewModel(scores: scores, bets: bets, poolId: "pool-a", gamblerId: "gambler-7")

        await viewModel.lazyPager.refresh()
        try await load(viewModel, scores, request: 1, with: .success(try timelineScore(3)))

        #expect(bets.requests == [GroupedFeedRequest(poolId: "pool-a", gamblerId: "gambler-7", next: nil)])
        #expect(scores.requests == [TimelineScoreRequest(poolId: "pool-a", gamblerId: "gambler-7")])
    }

    @Test
    func theTotalIsTheServerTotalNotTheSumOfLoadedAwards() async throws {
        let scores = TimelineGatedScoreRepository()
        // Three loaded awards of 5 points each: 15 in the rows.
        let bets = ScriptedGroupedFeedRepository(pages: [timelinePage(count: 3, next: "page-2")])
        let viewModel = makeViewModel(scores: scores, bets: bets)

        await viewModel.lazyPager.refresh()
        try await load(viewModel, scores, request: 1, with: .success(try timelineScore(120)))

        #expect(viewModel.pointsSummary.presentation == .points(.earned(120), .current))
    }

    @Test
    func appendingAPageFollowsTheCursorKeepsServerOrderAndNeitherRequestsNorChangesTheTotal() async throws {
        let scores = TimelineGatedScoreRepository()
        // Unsettled groups (earliest kickoff first), then finished groups (newest first), as the
        // server orders the phases.
        let bets = ScriptedGroupedFeedRepository(pages: [
            timelinePage(count: 2, prefix: "pending", firstHour: 1, hourStep: 1, computed: false, next: "page-2"),
            timelinePage(count: 2, prefix: "settled", firstHour: -1, hourStep: -1, next: nil),
        ])
        let viewModel = makeViewModel(scores: scores, bets: bets)
        try await load(viewModel, scores, request: 1, with: .success(try timelineScore(40)))
        await viewModel.lazyPager.refresh()
        let firstPage = viewModel.lazyPager.loadedItems

        await viewModel.lazyPager.onRowAccess(index: 1)

        #expect(bets.requests.map(\.next) == [nil, "page-2"])
        #expect(
            viewModel.lazyPager.loadedItems.map { $0.matches.map(\.matchId) }
                == [["pending-0-0", "pending-0-1"], ["pending-1-0", "pending-1-1"], ["settled-0-0", "settled-0-1"], ["settled-1-0", "settled-1-1"]]
        )
        #expect(viewModel.lazyPager.loadedItems.map(\.kickoffDateTime) == [1, 2, -1, -2].map { GroupFixtures.kickoff(hours: $0) })
        // The append changes no header, count, or movement of the groups already shown.
        #expect(Array(viewModel.lazyPager.loadedItems.prefix(2)) == firstPage)
        #expect(scores.requests.count == 1)
        #expect(viewModel.pointsSummary.presentation == .points(.earned(40), .current))
    }

    @Test
    func failedRowsLeaveTheLoadedTotalUsable() async throws {
        let scores = TimelineGatedScoreRepository()
        let bets = ScriptedGroupedFeedRepository(failsWith: URLError(.timedOut))
        let viewModel = makeViewModel(scores: scores, bets: bets)

        let refresh = Task { await viewModel.refreshListAndPointsSummary() }
        try await scores.waitForRequest(1)
        scores.complete(1, with: .success(try timelineScore(9)))
        await refresh.value

        #expect(viewModel.lazyPager.loadState.refresh.isFailure)
        #expect(viewModel.pointsSummary.presentation == .points(.earned(9), .current))
    }

    @Test
    func aFailedTotalLeavesRowsUsableAndRetryRequestsOnlyTheTotal() async throws {
        let scores = TimelineGatedScoreRepository()
        let bets = ScriptedGroupedFeedRepository(pages: [timelinePage(count: 3, next: nil)])
        let viewModel = makeViewModel(scores: scores, bets: bets)

        let refresh = Task { await viewModel.refreshListAndPointsSummary() }
        try await scores.waitForRequest(1)
        scores.complete(1, with: .failure(URLError(.notConnectedToInternet)))
        await refresh.value
        #expect(viewModel.pointsSummary.presentation == .failed)
        #expect(viewModel.lazyPager.itemCount == 3)

        let retry = Task { await viewModel.loadPointsSummary() }
        try await scores.waitForRequest(2)
        #expect(viewModel.pointsSummary.presentation == .placeholder)
        scores.complete(2, with: .success(try timelineScore(5)))
        await retry.value

        #expect(bets.requests.count == 1)
        #expect(viewModel.lazyPager.itemCount == 3)
        #expect(viewModel.pointsSummary.presentation == .points(.earned(5), .current))
    }

    @Test
    func aFailedRefreshKeepsThePreviousTotalWithTheFailure() async throws {
        let scores = TimelineGatedScoreRepository()
        let bets = ScriptedGroupedFeedRepository(pages: [timelinePage(count: 2, next: nil)])
        let viewModel = makeViewModel(scores: scores, bets: bets)
        try await load(viewModel, scores, request: 1, with: .success(try timelineScore(12)))

        let refresh = Task { await viewModel.refreshListAndPointsSummary() }
        try await scores.waitForRequest(2)
        #expect(viewModel.pointsSummary.presentation == .points(.earned(12), .refreshing))
        scores.complete(2, with: .failure(URLError(.timedOut)))
        await refresh.value

        #expect(viewModel.lazyPager.itemCount == 2)
        #expect(viewModel.pointsSummary.presentation == .points(.earned(12), .failed))
    }

    @Test
    func aSuccessfulRefreshWithoutATotalReplacesThePreviousNumber() async throws {
        let scores = TimelineGatedScoreRepository()
        let viewModel = makeViewModel(scores: scores)
        try await load(viewModel, scores, request: 1, with: .success(try timelineScore(12)))

        try await load(viewModel, scores, request: 2, with: .success(try timelineScore(nil)))

        #expect(viewModel.pointsSummary.presentation == .points(.unavailable, .current))
    }

    @Test
    func anOlderRefreshFinishingLastCannotReplaceTheNewerTotal() async throws {
        let scores = TimelineGatedScoreRepository()
        let viewModel = makeViewModel(scores: scores)

        let older = Task { await viewModel.loadPointsSummary() }
        try await scores.waitForRequest(1)
        let newer = Task { await viewModel.loadPointsSummary() }
        try await scores.waitForRequest(2)

        scores.complete(2, with: .success(try timelineScore(10)))
        await newer.value
        scores.complete(1, with: .failure(URLError(.timedOut)))
        await older.value

        #expect(viewModel.pointsSummary.presentation == .points(.earned(10), .current))
    }

    @Test
    func eachGamblerGetsOnlyTheirOwnTotal() async throws {
        let scores = TimelineGatedScoreRepository()
        let previous = makeViewModel(scores: scores, poolId: "pool-a", gamblerId: "gambler-1")
        let current = makeViewModel(scores: scores, poolId: "pool-a", gamblerId: "gambler-2")

        let pending = Task { await previous.loadPointsSummary() }
        try await scores.waitForRequest(1)
        let shown = Task { await current.loadPointsSummary() }
        try await scores.waitForRequest(2)

        scores.complete(2, with: .success(try timelineScore(7, gamblerId: "gambler-2")))
        await shown.value
        scores.complete(1, with: .success(try timelineScore(99, gamblerId: "gambler-1")))
        await pending.value

        #expect(scores.requests == [
            TimelineScoreRequest(poolId: "pool-a", gamblerId: "gambler-1"),
            TimelineScoreRequest(poolId: "pool-a", gamblerId: "gambler-2"),
        ])
        #expect(current.pointsSummary.presentation == .points(.earned(7), .current))
    }

    @Test
    func aRefreshThatFinishesAPartialGroupShowsItCompleteWithItsServerMovement() async throws {
        let scores = TimelineGatedScoreRepository()
        let partial = GroupFixtures.group("match", total: 2, computed: 1, movement: (8, 6))
        let bets = ScriptedGroupedFeedRepository(pages: [GroupFixtures.page([partial])])
        let viewModel = makeViewModel(scores: scores, bets: bets)
        await viewModel.lazyPager.refresh()
        let before = try #require(viewModel.lazyPager.peek(at: 0))
        #expect(before.movement == .moved(places: 2, partialProgress: KickoffGroupProgress(computedMatches: 1, totalMatches: 2)))
        #expect(before.matches.map { HistoryRowPoints($0) } == [.awarded(.earned(5)), .pending])

        bets.pages = [GroupFixtures.page([GroupFixtures.group("match", total: 2, computed: 2, movement: (8, 5))])]
        let refresh = Task { await viewModel.refreshListAndPointsSummary() }
        try await scores.waitForRequest(1)
        scores.complete(1, with: .success(try timelineScore(10)))
        await refresh.value

        let after = try #require(viewModel.lazyPager.peek(at: 0))
        #expect(viewModel.lazyPager.itemCount == 1)
        #expect(after.id == before.id)
        #expect(after.movement == .moved(places: 3, partialProgress: nil))
        #expect(after.matches.map { HistoryRowPoints($0) } == [.awarded(.earned(5)), .awarded(.earned(5))])
    }

    @Test
    func aGroupWithoutAComputedMatchIsPendingAndOneWithAMissingSnapshotIsUnavailable() async throws {
        let scores = TimelineGatedScoreRepository()
        let bets = ScriptedGroupedFeedRepository(pages: [GroupFixtures.page([
            GroupFixtures.group("pending", kickoff: GroupFixtures.kickoff(hours: 1), total: 2, computed: 0, movement: nil),
            GroupFixtures.group("legacy", total: 1, computed: 1, movement: nil),
        ])])
        let viewModel = makeViewModel(scores: scores, bets: bets)

        await viewModel.lazyPager.refresh()

        #expect(viewModel.lazyPager.loadedItems.map(\.movement) == [.pending, .unavailable])
    }

    @Test
    func listRetryRequestsOnlyTheRowsOnceAndPullToRefreshStillReloadsBoth() async throws {
        let scores = TimelineGatedScoreRepository()
        let bets = ScriptedGroupedFeedRepository(pages: [timelinePage(count: 2, next: nil)], failsWith: URLError(.timedOut))
        let viewModel = makeViewModel(scores: scores, bets: bets)
        try await load(viewModel, scores, request: 1, with: .success(try timelineScore(9)))
        await viewModel.lazyPager.refresh()
        #expect(viewModel.lazyPager.loadState.refresh.isFailure)

        bets.failure = nil
        var summaryRetries = 0
        let list = BetTimelineList(
            lazyPagingItems: viewModel.lazyPager,
            gamblerId: "gambler-id",
            gamblerUsername: "El mono",
            pointsSummary: viewModel.pointsSummary,
            onPointsSummaryRetry: { summaryRetries += 1 },
            onRetryFirstPage: { Task { await viewModel.retryFirstPage() } },
            onRetryPage: { _ in }
        )
        try list.inspect().find(ViewType.Button.self, where: { try $0.labelView().text().string() == "Retry" }).tap()
        try await waitUntil { viewModel.lazyPager.itemCount == 2 }

        #expect(bets.requests.count == 2)
        #expect(scores.requests.count == 1)
        #expect(summaryRetries == 0)
        #expect(viewModel.pointsSummary.presentation == .points(.earned(9), .current))

        let refresh = Task { await viewModel.refreshListAndPointsSummary() }
        try await scores.waitForRequest(2)
        scores.complete(2, with: .success(try timelineScore(11)))
        await refresh.value

        #expect(bets.requests.count == 3)
        #expect(viewModel.lazyPager.itemCount == 2)
        #expect(viewModel.pointsSummary.presentation == .points(.earned(11), .current))
    }

    /// The list's Retry, driven through the view model so it runs where ViewInspector cannot: it
    /// requests the first page again and never the total.
    @Test
    func firstPageRetryRequestsOnlyTheRowsAndKeepsTheTotal() async throws {
        let scores = TimelineGatedScoreRepository()
        let bets = ScriptedGroupedFeedRepository(pages: [timelinePage(count: 2, next: nil)], failsWith: URLError(.timedOut))
        let viewModel = makeViewModel(scores: scores, bets: bets)
        try await load(viewModel, scores, request: 1, with: .success(try timelineScore(9)))
        await viewModel.lazyPager.refresh()
        #expect(viewModel.lazyPager.loadState.refresh.isFailure)

        bets.failure = nil
        await viewModel.retryFirstPage()
        try await waitUntil { viewModel.lazyPager.itemCount == 2 }

        #expect(bets.requests.map(\.next) == [nil, nil])
        #expect(scores.requests.count == 1)
        #expect(viewModel.pointsSummary.presentation == .points(.earned(9), .current))
    }

    // MARK: Helpers

    private func load(
        _ viewModel: BetTimelineListViewModel,
        _ scores: TimelineGatedScoreRepository,
        request: Int,
        with result: Result<PoolGamblerScore, Error>
    ) async throws {
        let load = Task { await viewModel.loadPointsSummary() }
        try await scores.waitForRequest(request)
        scores.complete(request, with: result)
        await load.value
    }

    /// Budgets polls, not wall time: a busy CI main actor can stall this task far past any deadline.
    private func waitUntil(polls: Int = 500, _ condition: () -> Bool) async throws {
        var remaining = polls
        while !condition() {
            guard remaining > 0 else { throw TimelineConditionTimeout() }
            remaining -= 1
            try await Task.sleep(for: .milliseconds(10))
        }
    }

    private func makeViewModel(
        scores: TimelineGatedScoreRepository,
        bets: ScriptedGroupedFeedRepository? = nil,
        poolId: String = "pool-id",
        gamblerId: String = "gambler-id"
    ) -> BetTimelineListViewModel {
        BetTimelineListViewModel(
            getGamblerBetsTimelineUseCase: GetGamblerBetsTimelineUseCase(
                poolGamblerBetRepository: bets ?? ScriptedGroupedFeedRepository()
            ),
            getPoolGamblerScoreUseCase: GetPoolGamblerScoreUseCase(poolGamblerScoreRepository: scores),
            poolId: poolId,
            gamblerId: gamblerId
        )
    }
}

/// `PoolGamblerScore`'s memberwise initializer is internal to `DataPool`; its Codable shape is
/// public.
private func timelineScore(
    _ value: Int?,
    poolId: String = "pool-id",
    gamblerId: String = "gambler-id"
) throws -> PoolGamblerScore {
    let json: [String: Any?] = [
        "poolId": poolId,
        "poolName": "Pool",
        "gamblerId": gamblerId,
        "gamblerUsername": "gambler",
        "position": 1,
        "beforePosition": 1,
        "score": value,
        "gamblerCount": 13,
    ]
    let data = try JSONSerialization.data(withJSONObject: json.compactMapValues { $0 })
    return try JSONDecoder().decode(PoolGamblerScore.self, from: data)
}

/// One page of `count` two-match kickoff groups whose rows award 5 points each once computed,
/// `hourStep` hours apart from `firstHour`.
private func timelinePage(
    count: Int,
    prefix: String = "match",
    firstHour: Int = 0,
    hourStep: Int = -1,
    computed: Bool = true,
    next: String?
) -> CursorPage<KickoffGroup> {
    GroupFixtures.page(
        (0..<count).map { index in
            GroupFixtures.group(
                "\(prefix)-\(index)",
                kickoff: GroupFixtures.kickoff(hours: firstHour + index * hourStep),
                computed: computed ? 2 : 0,
                movement: computed ? (8, 5) : nil
            )
        },
        next: next
    )
}

private struct TimelineScoreRequest: Equatable {
    let poolId: String
    let gamblerId: String
}

private struct TimelineConditionTimeout: Error {}

/// Holds each score request until the test completes it by its 1-based order, so requests can
/// finish in any order.
@MainActor
private final class TimelineGatedScoreRepository: PoolGamblerScoreRepository {
    private(set) var requests: [TimelineScoreRequest] = []
    private var pending: [Int: CheckedContinuation<Result<PoolGamblerScore, Error>, Never>] = [:]

    /// Budgets polls, not wall time: a busy CI main actor can stall this task far past any deadline.
    func waitForRequest(_ number: Int, polls: Int = 500) async throws {
        var remaining = polls
        while pending[number] == nil {
            guard remaining > 0 else { throw TimelineConditionTimeout() }
            remaining -= 1
            try await Task.sleep(for: .milliseconds(10))
        }
    }

    func complete(_ number: Int, with result: Result<PoolGamblerScore, Error>) {
        pending.removeValue(forKey: number)?.resume(returning: result)
    }

    nonisolated func getPoolGamblerScore(
        poolId: String,
        gamblerId: String
    ) async -> Result<PoolGamblerScore, Error> {
        await withCheckedContinuation { continuation in
            Task { @MainActor in
                self.requests.append(TimelineScoreRequest(poolId: poolId, gamblerId: gamblerId))
                self.pending[self.requests.count] = continuation
            }
        }
    }

    nonisolated func getPoolGamblerScoresByGambler(
        gamblerId: String, next: String?, searchText: String?
    ) async -> Result<CursorPage<PoolGamblerScore>, Error> { .failure(Unused()) }

    nonisolated func getPoolGamblerScoresByPool(
        poolId: String, next: String?, searchText: String?
    ) async -> Result<CursorPage<PoolGamblerScore>, Error> { .failure(Unused()) }

    private struct Unused: Error {}
}
