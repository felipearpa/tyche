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
        let bets = ScriptedGroupedFeedRepository(pages: [historyPage(count: 3, next: nil)])
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
        let bets = ScriptedGroupedFeedRepository(pages: [
            historyPage(count: 3, prefix: "first", next: "page-2"),
            historyPage(count: 3, prefix: "second", firstHour: -3, next: nil),
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
        let bets = ScriptedGroupedFeedRepository(pages: [historyPage(count: 2, next: nil)])
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
        let bets = ScriptedGroupedFeedRepository(pages: [historyPage(count: 2, next: nil)])
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
        let bets = ScriptedGroupedFeedRepository(pages: [historyPage(count: 2, next: nil)], failsWith: URLError(.timedOut))
        let viewModel = makeViewModel(scores: scores, bets: bets)
        try await load(viewModel, scores, request: 1, with: .success(try score(9)))
        await viewModel.lazyPager.refresh()
        #expect(viewModel.lazyPager.loadState.refresh.isFailure)

        bets.failure = nil
        var summaryRetries = 0
        let list = FinishedBetList(
            lazyPagingItems: viewModel.lazyPager,
            pointsSummary: viewModel.pointsSummary,
            onPointsSummaryRetry: { summaryRetries += 1 },
            onRetryFirstPage: { Task { await viewModel.retryFirstPage() } },
            onRetryPage: { _ in }
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

    /// The list's Retry, driven through the view model so it runs where ViewInspector cannot: it
    /// requests the first page again and never the total.
    @Test
    func firstPageRetryRequestsOnlyTheRowsAndKeepsTheTotal() async throws {
        let scores = GatedScoreRepository()
        let bets = ScriptedGroupedFeedRepository(pages: [historyPage(count: 2, next: nil)], failsWith: URLError(.timedOut))
        let viewModel = makeViewModel(scores: scores, bets: bets)
        try await load(viewModel, scores, request: 1, with: .success(try score(9)))
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
        bets: ScriptedGroupedFeedRepository? = nil,
        poolId: String = "pool-id",
        gamblerId: String = "gambler-id"
    ) -> FinishedBetListViewModel {
        FinishedBetListViewModel(
            getFinishedPoolGamblerBetsUseCase: GetFinishedPoolGamblerBetsUseCase(
                poolGamblerBetRepository: bets ?? ScriptedGroupedFeedRepository()
            ),
            getPoolGamblerScoreUseCase: GetPoolGamblerScoreUseCase(poolGamblerScoreRepository: scores),
            gamblerId: gamblerId,
            poolId: poolId
        )
    }

    /// Budgets polls, not wall time: a busy CI main actor can stall this task far past any deadline.
    private func waitUntil(
        polls: Int = 500,
        _ condition: () -> Bool
    ) async throws {
        var remaining = polls
        while !condition() {
            guard remaining > 0 else { throw ConditionTimeout() }
            remaining -= 1
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

/// One page of `count` kickoff groups, newest first and an hour apart from `firstHour`.
private func historyPage(count: Int, prefix: String = "match", firstHour: Int = 0, next: String?) -> CursorPage<KickoffGroup> {
    GroupFixtures.page(
        (0..<count).map { index in
            GroupFixtures.group("\(prefix)-\(index)", kickoff: GroupFixtures.kickoff(hours: firstHour - index))
        },
        next: next
    )
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

    /// Budgets polls, not wall time: a busy CI main actor can stall this task far past any deadline.
    func waitForRequest(_ number: Int, polls: Int = 500) async throws {
        var remaining = polls
        while pending[number] == nil {
            guard remaining > 0 else { throw ConditionTimeout() }
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
