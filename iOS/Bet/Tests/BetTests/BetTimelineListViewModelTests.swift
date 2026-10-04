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
        let bets = TimelineBetRepository(pages: [timelinePage(count: 2, next: nil)])
        let viewModel = makeViewModel(scores: scores, bets: bets, poolId: "pool-a", gamblerId: "gambler-7")

        await viewModel.lazyPager.refresh()
        try await load(viewModel, scores, request: 1, with: .success(try timelineScore(3)))

        #expect(bets.requests == [TimelineBetRequest(poolId: "pool-a", gamblerId: "gambler-7", next: nil)])
        #expect(scores.requests == [TimelineScoreRequest(poolId: "pool-a", gamblerId: "gambler-7")])
    }

    @Test
    func theTotalIsTheServerTotalNotTheSumOfLoadedAwards() async throws {
        let scores = TimelineGatedScoreRepository()
        // Three loaded awards of 5 points each: 15 in the rows.
        let bets = TimelineBetRepository(pages: [timelinePage(count: 3, next: "page-2")])
        let viewModel = makeViewModel(scores: scores, bets: bets)

        await viewModel.lazyPager.refresh()
        try await load(viewModel, scores, request: 1, with: .success(try timelineScore(120)))

        #expect(viewModel.pointsSummary.presentation == .points(.earned(120), .current))
    }

    @Test
    func appendingAPageFollowsTheCursorAndNeitherRequestsNorChangesTheTotal() async throws {
        let scores = TimelineGatedScoreRepository()
        let bets = TimelineBetRepository(pages: [
            timelinePage(count: 2, prefix: "pending", computed: false, next: "page-2"),
            timelinePage(count: 2, prefix: "settled", next: nil),
        ])
        let viewModel = makeViewModel(scores: scores, bets: bets)
        try await load(viewModel, scores, request: 1, with: .success(try timelineScore(40)))
        await viewModel.lazyPager.refresh()

        await viewModel.lazyPager.onRowAccess(index: 1)

        #expect(bets.requests.map(\.next) == [nil, "page-2"])
        #expect(viewModel.lazyPager.loadedItems.map(\.matchId) == ["pending-0", "pending-1", "settled-0", "settled-1"])
        #expect(viewModel.lazyPager.loadedItems.map(\.isComputed) == [false, false, true, true])
        #expect(scores.requests.count == 1)
        #expect(viewModel.pointsSummary.presentation == .points(.earned(40), .current))
    }

    @Test
    func failedRowsLeaveTheLoadedTotalUsable() async throws {
        let scores = TimelineGatedScoreRepository()
        let bets = TimelineBetRepository(pages: [], failsWith: URLError(.timedOut))
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
        let bets = TimelineBetRepository(pages: [timelinePage(count: 3, next: nil)])
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
        let bets = TimelineBetRepository(pages: [timelinePage(count: 2, next: nil)])
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
    func aRefreshThatReturnsAnEntryAsComputedShowsItsAward() async throws {
        let scores = TimelineGatedScoreRepository()
        let bets = TimelineBetRepository(pages: [timelinePage(count: 1, prefix: "match", computed: false, next: nil)])
        let viewModel = makeViewModel(scores: scores, bets: bets)
        await viewModel.lazyPager.refresh()
        let pending = try #require(viewModel.lazyPager.peek(at: 0))
        #expect(HistoryRowPoints(pending) == .pending)

        bets.pages = [timelinePage(count: 1, prefix: "match", computed: true, next: nil)]
        let refresh = Task { await viewModel.refreshListAndPointsSummary() }
        try await scores.waitForRequest(1)
        scores.complete(1, with: .success(try timelineScore(5)))
        await refresh.value

        let settled = try #require(viewModel.lazyPager.peek(at: 0))
        #expect(settled.matchId == pending.matchId)
        #expect(HistoryRowPoints(settled) == .awarded(.earned(5)))
    }

    @Test
    func listRetryRequestsOnlyTheRowsOnceAndPullToRefreshStillReloadsBoth() async throws {
        let scores = TimelineGatedScoreRepository()
        let bets = TimelineBetRepository(pages: [timelinePage(count: 2, next: nil)], failsWith: URLError(.timedOut))
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
            onPointsSummaryRetry: { summaryRetries += 1 }
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

    private func waitUntil(timeout: Duration = .seconds(5), _ condition: () -> Bool) async throws {
        let deadline = ContinuousClock.now.advanced(by: timeout)
        while !condition() {
            guard ContinuousClock.now < deadline else { throw TimelineConditionTimeout() }
            try await Task.sleep(for: .milliseconds(10))
        }
    }

    private func makeViewModel(
        scores: TimelineGatedScoreRepository,
        bets: TimelineBetRepository? = nil,
        poolId: String = "pool-id",
        gamblerId: String = "gambler-id"
    ) -> BetTimelineListViewModel {
        BetTimelineListViewModel(
            getGamblerBetsTimelineUseCase: GetGamblerBetsTimelineUseCase(
                poolGamblerBetRepository: bets ?? TimelineBetRepository(pages: [])
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

/// Entries award 5 points each once computed; uncomputed entries carry no award.
private func timelinePage(
    count: Int,
    prefix: String = "match",
    computed: Bool = true,
    next: String?
) -> CursorPage<PoolGamblerBet> {
    CursorPage(
        items: (0..<count).map { index in
            timelineBet(poolGamblerBetDummyModel().copy {
                $0.matchId = "\(prefix)-\(index)"
                $0.isComputed = computed
                $0.score = computed ? 5 : nil
            })
        },
        next: next
    )
}

/// `PoolGamblerBet`'s memberwise initializer is internal to `DataBet`; both types share their
/// Codable shape.
private func timelineBet(_ model: PoolGamblerBetModel) -> PoolGamblerBet {
    try! JSONDecoder().decode(PoolGamblerBet.self, from: JSONEncoder().encode(model))
}

private struct TimelineScoreRequest: Equatable {
    let poolId: String
    let gamblerId: String
}

private struct TimelineBetRequest: Equatable {
    let poolId: String
    let gamblerId: String
    let next: String?
}

private struct TimelineConditionTimeout: Error {}

/// Holds each score request until the test completes it by its 1-based order, so requests can
/// finish in any order.
@MainActor
private final class TimelineGatedScoreRepository: PoolGamblerScoreRepository {
    private(set) var requests: [TimelineScoreRequest] = []
    private var pending: [Int: CheckedContinuation<Result<PoolGamblerScore, Error>, Never>] = [:]

    func waitForRequest(_ number: Int, timeout: Duration = .seconds(5)) async throws {
        let deadline = ContinuousClock.now.advanced(by: timeout)
        while pending[number] == nil {
            guard ContinuousClock.now < deadline else { throw TimelineConditionTimeout() }
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

/// Serves Timeline pages in order: the first request (no cursor) gets the first page, and a
/// request with a cursor gets the page after it. `pages` can change between refreshes.
@MainActor
private final class TimelineBetRepository: PoolGamblerBetRepository {
    var pages: [CursorPage<PoolGamblerBet>]
    var failure: Error?
    private(set) var requests: [TimelineBetRequest] = []

    init(pages: [CursorPage<PoolGamblerBet>], failsWith failure: Error? = nil) {
        self.pages = pages
        self.failure = failure
    }

    private func page(poolId: String, gamblerId: String, after next: String?) -> Result<CursorPage<PoolGamblerBet>, Error> {
        requests.append(TimelineBetRequest(poolId: poolId, gamblerId: gamblerId, next: next))
        if let failure { return .failure(failure) }
        guard let next else { return .success(pages.first ?? CursorPage(items: [], next: nil)) }
        let index = pages.firstIndex { $0.next == next }.map { $0 + 1 } ?? pages.count
        return .success(index < pages.count ? pages[index] : CursorPage(items: [], next: nil))
    }

    nonisolated func getGamblerBetsTimeline(
        poolId: String, gamblerId: String, next: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> {
        await page(poolId: poolId, gamblerId: gamblerId, after: next)
    }

    nonisolated func getFinishedPoolGamblerBets(
        poolId: String, gamblerId: String, next: String?, searchText: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> { .failure(Unused()) }

    nonisolated func getPoolGamblerBet(
        poolId: String, gamblerId: String, matchId: String
    ) async -> Result<PoolGamblerBet, Error> { .failure(Unused()) }

    nonisolated func getPendingPoolGamblerBets(
        poolId: String, gamblerId: String, next: String?, searchText: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> { .failure(Unused()) }

    nonisolated func getLivePoolGamblerBets(
        poolId: String, gamblerId: String, next: String?, searchText: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> { .failure(Unused()) }

    nonisolated func getPoolMatchGamblerBets(
        poolId: String, matchId: String, next: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> { .failure(Unused()) }

    nonisolated func bet(bet: Bet) async -> Result<PoolGamblerBet, Error> { .failure(Unused()) }

    private struct Unused: Error {}
}
