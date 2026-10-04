import Core
import DataBet
import DataPool
import Foundation
import Testing
import UI
import ViewInspector
@testable import Bet

/// History's earned-points total, as explored on the iOS 18.1 simulator and the Android phone: it
/// loads on entry and with pull to refresh, keeps the confirmed total while a refresh runs or
/// fails, and is independent of the paged rows. Failures, out-of-order responses, and context
/// changes cannot be staged with real data, so these tests cover them.
@MainActor
struct FinishedBetListViewModelPointsSummaryTests {
    @Test
    func firstLoadShowsThePlaceholderUntilAPositiveTotalArrives() async throws {
        let scores = GatedScoreRepository()
        let viewModel = makeViewModel(scores: scores)

        let load = Task { await viewModel.loadPointsSummary() }
        try await scores.waitForRequest(1)
        #expect(viewModel.pointsSummary.presentation == .placeholder)

        scores.complete(1, with: .success(try score(120)))
        await load.value
        #expect(viewModel.pointsSummary.presentation == .points(.earned(120), .current))
    }

    @Test
    func aZeroTotalIsAConfirmedValueNotAnUnavailableOne() async throws {
        let scores = GatedScoreRepository()
        let viewModel = makeViewModel(scores: scores)

        try await load(viewModel, scores, request: 1, with: .success(try score(0)))

        #expect(viewModel.pointsSummary.presentation == .points(.earned(0), .current))
    }

    @Test
    func aTotalMissingFromASuccessfulResponseReplacesThePreviousNumber() async throws {
        let scores = GatedScoreRepository()
        let viewModel = makeViewModel(scores: scores)
        try await load(viewModel, scores, request: 1, with: .success(try score(40)))

        try await load(viewModel, scores, request: 2, with: .success(try score(nil)))

        #expect(viewModel.pointsSummary.presentation == .points(.unavailable, .current))
    }

    @Test
    func aFailedFirstLoadShowsTheErrorAndRetryShowsThePlaceholderAgain() async throws {
        let scores = GatedScoreRepository()
        let viewModel = makeViewModel(scores: scores)
        try await load(viewModel, scores, request: 1, with: .failure(URLError(.notConnectedToInternet)))
        #expect(viewModel.pointsSummary.presentation == .failed)

        let retry = Task { await viewModel.loadPointsSummary() }
        try await scores.waitForRequest(2)
        #expect(viewModel.pointsSummary.presentation == .placeholder)

        scores.complete(2, with: .success(try score(4)))
        await retry.value
        #expect(viewModel.pointsSummary.presentation == .points(.earned(4), .current))
    }

    @Test
    func aRefreshKeepsTheTotalVisibleAndAFailedRefreshKeepsItWithTheFailure() async throws {
        let scores = GatedScoreRepository()
        let viewModel = makeViewModel(scores: scores)
        try await load(viewModel, scores, request: 1, with: .success(try score(4)))

        let refresh = Task { await viewModel.loadPointsSummary() }
        try await scores.waitForRequest(2)
        #expect(viewModel.pointsSummary.presentation == .points(.earned(4), .refreshing))

        scores.complete(2, with: .failure(URLError(.timedOut)))
        await refresh.value
        #expect(viewModel.pointsSummary.presentation == .points(.earned(4), .failed))
    }

    @Test
    func anOlderResponseFinishingLastDoesNotReplaceTheNewerOne() async throws {
        let scores = GatedScoreRepository()
        let viewModel = makeViewModel(scores: scores)

        let older = Task { await viewModel.loadPointsSummary() }
        try await scores.waitForRequest(1)
        let newer = Task { await viewModel.loadPointsSummary() }
        try await scores.waitForRequest(2)

        scores.complete(2, with: .success(try score(10)))
        await newer.value
        scores.complete(1, with: .success(try score(3)))
        await older.value

        #expect(viewModel.pointsSummary.presentation == .points(.earned(10), .current))
    }

    @Test
    func anOlderFailureFinishingLastDoesNotMarkTheNewerTotalAsFailed() async throws {
        let scores = GatedScoreRepository()
        let viewModel = makeViewModel(scores: scores)

        let older = Task { await viewModel.loadPointsSummary() }
        try await scores.waitForRequest(1)
        let newer = Task { await viewModel.loadPointsSummary() }
        try await scores.waitForRequest(2)

        scores.complete(2, with: .success(try score(10)))
        await newer.value
        scores.complete(1, with: .failure(URLError(.timedOut)))
        await older.value

        #expect(viewModel.pointsSummary.presentation == .points(.earned(10), .current))
    }

    @Test
    func eachContextRequestsItsOwnTotalAndNeverShowsAnotherContextsTotal() async throws {
        let scores = GatedScoreRepository()
        let firstPool = makeViewModel(scores: scores, poolId: "pool-a", gamblerId: "gambler-1")
        let secondPool = makeViewModel(scores: scores, poolId: "pool-b", gamblerId: "gambler-1")

        let pending = Task { await firstPool.loadPointsSummary() }
        try await scores.waitForRequest(1)
        let current = Task { await secondPool.loadPointsSummary() }
        try await scores.waitForRequest(2)

        scores.complete(2, with: .success(try score(7, poolId: "pool-b")))
        await current.value
        scores.complete(1, with: .success(try score(99, poolId: "pool-a")))
        await pending.value

        #expect(scores.requests == [
            ScoreRequest(poolId: "pool-a", gamblerId: "gambler-1"),
            ScoreRequest(poolId: "pool-b", gamblerId: "gambler-1"),
        ])
        #expect(secondPool.pointsSummary.presentation == .points(.earned(7), .current))
    }

    @Test
    func summaryRetryDoesNotReloadTheRows() async throws {
        let scores = GatedScoreRepository()
        let bets = PagedBetRepository(pages: [historyPage(count: 3, next: nil)])
        let viewModel = makeViewModel(scores: scores, bets: bets)
        await viewModel.lazyPager.refresh()
        let betRequestsBeforeRetry = bets.requestCount

        try await load(viewModel, scores, request: 1, with: .failure(URLError(.timedOut)))
        try await load(viewModel, scores, request: 2, with: .success(try score(5)))

        #expect(bets.requestCount == betRequestsBeforeRetry)
        #expect(viewModel.lazyPager.itemCount == 3)
    }

    @Test
    func appendingAPageNeitherRequestsNorChangesTheTotal() async throws {
        let scores = GatedScoreRepository()
        let bets = PagedBetRepository(pages: [
            historyPage(count: 3, prefix: "first", next: "page-2"),
            historyPage(count: 3, prefix: "second", next: nil),
        ])
        let viewModel = makeViewModel(scores: scores, bets: bets)
        try await load(viewModel, scores, request: 1, with: .success(try score(120)))
        await viewModel.lazyPager.refresh()

        await viewModel.lazyPager.onRowAccess(index: 2)

        #expect(viewModel.lazyPager.itemCount == 6)
        #expect(bets.requestCount == 2)
        #expect(scores.requests.count == 1)
        #expect(viewModel.pointsSummary.presentation == .points(.earned(120), .current))
    }

    @Test
    func pullToRefreshReloadsBothAndWaitsForTheSlowerTotal() async throws {
        let scores = GatedScoreRepository()
        let bets = PagedBetRepository(pages: [historyPage(count: 2, next: nil)])
        let viewModel = makeViewModel(scores: scores, bets: bets)

        let refresh = Task { await viewModel.refreshListAndPointsSummary() }
        try await scores.waitForRequest(1)
        try await waitUntil { viewModel.lazyPager.itemCount == 2 }

        // The rows arrived without waiting for the total, and the refresh is still running.
        #expect(bets.requestCount == 1)
        #expect(viewModel.pointsSummary.isLoading)

        scores.complete(1, with: .success(try score(9)))
        await refresh.value
        #expect(viewModel.pointsSummary.presentation == .points(.earned(9), .current))
    }

    @Test
    func aFailedTotalDuringPullToRefreshKeepsTheRefreshedRows() async throws {
        let scores = GatedScoreRepository()
        let bets = PagedBetRepository(pages: [historyPage(count: 2, next: nil)])
        let viewModel = makeViewModel(scores: scores, bets: bets)

        let refresh = Task { await viewModel.refreshListAndPointsSummary() }
        try await scores.waitForRequest(1)
        scores.complete(1, with: .failure(URLError(.timedOut)))
        await refresh.value

        #expect(viewModel.lazyPager.itemCount == 2)
        #expect(viewModel.pointsSummary.presentation == .failed)
    }

    @Test
    func listRetryRequestsOnlyTheRowsOnceAndPullToRefreshStillReloadsBoth() async throws {
        let scores = GatedScoreRepository()
        let bets = PagedBetRepository(pages: [historyPage(count: 2, next: nil)], failsWith: URLError(.timedOut))
        let viewModel = makeViewModel(scores: scores, bets: bets)
        try await load(viewModel, scores, request: 1, with: .success(try score(9)))
        await viewModel.lazyPager.refresh()
        #expect(viewModel.lazyPager.loadState.refresh.isFailure)

        bets.failure = nil
        var summaryRetries = 0
        let list = FinishedBetList(
            lazyPagingItems: viewModel.lazyPager,
            pointsSummary: viewModel.pointsSummary,
            onPointsSummaryRetry: { summaryRetries += 1 }
        )
        try list.inspect().find(ViewType.Button.self, where: { try $0.labelView().text().string() == "Retry" }).tap()
        try await waitUntil { viewModel.lazyPager.itemCount == 2 }

        #expect(bets.requestCount == 2)
        #expect(scores.requests.count == 1)
        #expect(summaryRetries == 0)
        #expect(viewModel.pointsSummary.presentation == .points(.earned(9), .current))

        let refresh = Task { await viewModel.refreshListAndPointsSummary() }
        try await scores.waitForRequest(2)
        scores.complete(2, with: .success(try score(11)))
        await refresh.value

        #expect(bets.requestCount == 3)
        #expect(viewModel.lazyPager.itemCount == 2)
        #expect(viewModel.pointsSummary.presentation == .points(.earned(11), .current))
    }

    // MARK: Helpers

    private func load(
        _ viewModel: FinishedBetListViewModel,
        _ scores: GatedScoreRepository,
        request: Int,
        with result: Result<PoolGamblerScore, Error>
    ) async throws {
        let load = Task { await viewModel.loadPointsSummary() }
        try await scores.waitForRequest(request)
        scores.complete(request, with: result)
        await load.value
    }

    private func makeViewModel(
        scores: GatedScoreRepository,
        bets: PagedBetRepository? = nil,
        poolId: String = "pool-id",
        gamblerId: String = "gambler-id"
    ) -> FinishedBetListViewModel {
        FinishedBetListViewModel(
            getFinishedPoolGamblerBetsUseCase: GetFinishedPoolGamblerBetsUseCase(
                poolGamblerBetRepository: bets ?? PagedBetRepository(pages: [])
            ),
            getPoolGamblerScoreUseCase: GetPoolGamblerScoreUseCase(poolGamblerScoreRepository: scores),
            gamblerId: gamblerId,
            poolId: poolId
        )
    }

    private func waitUntil(
        timeout: Duration = .seconds(5),
        _ condition: () -> Bool
    ) async throws {
        let deadline = ContinuousClock.now.advanced(by: timeout)
        while !condition() {
            guard ContinuousClock.now < deadline else { throw ConditionTimeout() }
            try await Task.sleep(for: .milliseconds(10))
        }
    }
}

/// `PoolGamblerScore`'s memberwise initializer is internal to `DataPool`; its Codable shape is
/// public.
private func score(
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

private func historyPage(count: Int, prefix: String = "match", next: String?) -> CursorPage<PoolGamblerBet> {
    CursorPage(
        items: (0..<count).map { index in
            historyBet(poolGamblerBetDummyModel().copy { $0.matchId = "\(prefix)-\(index)" })
        },
        next: next
    )
}

/// `PoolGamblerBet`'s memberwise initializer is internal to `DataBet`; both types share their
/// Codable shape.
private func historyBet(_ model: PoolGamblerBetModel) -> PoolGamblerBet {
    try! JSONDecoder().decode(PoolGamblerBet.self, from: JSONEncoder().encode(model))
}

private struct ScoreRequest: Equatable {
    let poolId: String
    let gamblerId: String
}

private struct ConditionTimeout: Error {}

/// Holds each score request until the test completes it by its 1-based order, so requests can
/// finish in any order.
@MainActor
private final class GatedScoreRepository: PoolGamblerScoreRepository {
    private(set) var requests: [ScoreRequest] = []
    private var pending: [Int: CheckedContinuation<Result<PoolGamblerScore, Error>, Never>] = [:]

    func waitForRequest(_ number: Int, timeout: Duration = .seconds(5)) async throws {
        let deadline = ContinuousClock.now.advanced(by: timeout)
        while pending[number] == nil {
            guard ContinuousClock.now < deadline else { throw ConditionTimeout() }
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
                self.requests.append(ScoreRequest(poolId: poolId, gamblerId: gamblerId))
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

/// Serves finished-bet pages in order: the first request (no cursor) gets the first page, and a
/// request with a cursor gets the page after it.
@MainActor
private final class PagedBetRepository: PoolGamblerBetRepository {
    private let pages: [CursorPage<PoolGamblerBet>]
    /// While set, every request fails with it.
    var failure: Error?
    private(set) var requestCount = 0

    init(pages: [CursorPage<PoolGamblerBet>], failsWith failure: Error? = nil) {
        self.pages = pages
        self.failure = failure
    }

    private func page(after next: String?) -> Result<CursorPage<PoolGamblerBet>, Error> {
        requestCount += 1
        if let failure { return .failure(failure) }
        guard let next else { return .success(pages.first ?? CursorPage(items: [], next: nil)) }
        let index = pages.firstIndex { $0.next == next }.map { $0 + 1 } ?? pages.count
        return .success(index < pages.count ? pages[index] : CursorPage(items: [], next: nil))
    }

    nonisolated func getFinishedPoolGamblerBets(
        poolId: String, gamblerId: String, next: String?, searchText: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> {
        await page(after: next)
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

    nonisolated func getPoolMatchGamblerBets(
        poolId: String, matchId: String, next: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> { .failure(Unused()) }

    nonisolated func getGamblerBetsTimeline(
        poolId: String, gamblerId: String, next: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> { .failure(Unused()) }

    nonisolated func bet(bet: Bet) async -> Result<PoolGamblerBet, Error> { .failure(Unused()) }

    private struct Unused: Error {}
}
