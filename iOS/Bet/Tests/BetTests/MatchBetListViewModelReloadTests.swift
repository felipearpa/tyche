import Core
import DataBet
import Foundation
import Testing
import ViewingState
@testable import Bet

/// The match screen's header state across reloads, as explored on the iOS 18.1 simulator: the
/// first load shows the placeholder header, while returning from a gambler's timeline or pulling
/// to refresh reloads the match without replacing the loaded header with placeholders.
@MainActor
struct MatchBetListViewModelReloadTests {
    @Test
    func firstLoadShowsLoadingUntilTheMatchArrives() async throws {
        let repository = GatedMatchRepository()
        let viewModel = makeViewModel(repository)

        let load = Task { await viewModel.loadPoolGamblerBet() }
        try await repository.waitForRequest(1)
        #expect(viewModel.poolGamblerBetState.isLoading())

        repository.complete(with: .success(try matchBet()))
        await load.value
        #expect(viewModel.poolGamblerBetState.isLoaded())
    }

    @Test
    func reloadKeepsTheLoadedMatchWhileTheRequestRuns() async throws {
        let repository = GatedMatchRepository()
        let viewModel = makeViewModel(repository)
        try await loadOnce(viewModel, repository)

        let reload = Task { await viewModel.loadPoolGamblerBet() }
        try await repository.waitForRequest(2)
        #expect(viewModel.poolGamblerBetState.isLoaded())

        repository.complete(with: .success(try matchBet()))
        await reload.value
        #expect(viewModel.poolGamblerBetState.isLoaded())
    }

    @Test
    func failedReloadStillShowsTheExistingFailureAndRetryShowsLoading() async throws {
        let repository = GatedMatchRepository()
        let viewModel = makeViewModel(repository)
        try await loadOnce(viewModel, repository)

        let reload = Task { await viewModel.loadPoolGamblerBet() }
        try await repository.waitForRequest(2)
        repository.complete(with: .failure(URLError(.notConnectedToInternet)))
        await reload.value
        #expect(viewModel.poolGamblerBetState.isFailure())

        let retry = Task { await viewModel.loadPoolGamblerBet() }
        try await repository.waitForRequest(3)
        #expect(viewModel.poolGamblerBetState.isLoading())

        repository.complete(with: .success(try matchBet()))
        await retry.value
        #expect(viewModel.poolGamblerBetState.isLoaded())
    }

    private func loadOnce(
        _ viewModel: MatchBetListViewModel,
        _ repository: GatedMatchRepository
    ) async throws {
        let load = Task { await viewModel.loadPoolGamblerBet() }
        try await repository.waitForRequest(1)
        repository.complete(with: .success(try matchBet()))
        await load.value
    }

    private func makeViewModel(_ repository: GatedMatchRepository) -> MatchBetListViewModel {
        MatchBetListViewModel(
            getPoolGamblerBetUseCase: GetPoolGamblerBetUseCase(poolGamblerBetRepository: repository),
            getPoolMatchGamblerBetsUseCase: GetPoolMatchGamblerBetsUseCase(
                poolGamblerBetRepository: repository
            ),
            poolId: "pool-id",
            gamblerId: "gambler-id",
            matchId: "match-id"
        )
    }

    /// The repository returns the domain type, whose memberwise initializer is internal to
    /// `DataBet`; both types share their Codable shape.
    private func matchBet() throws -> PoolGamblerBet {
        try JSONDecoder().decode(
            PoolGamblerBet.self,
            from: JSONEncoder().encode(poolGamblerBetDummyModel())
        )
    }
}

/// Holds each match request until the test completes it, so the state during the request can be
/// observed. Only `getPoolGamblerBet` is used by these tests.
@MainActor
private final class GatedMatchRepository: PoolGamblerBetRepository {
    private var requestCount = 0
    private var pending: CheckedContinuation<Result<PoolGamblerBet, Error>, Never>?

    /// Budgets polls, not wall time: a busy CI main actor can stall this task far past any deadline.
    func waitForRequest(_ count: Int, polls: Int = 500) async throws {
        var remaining = polls
        while requestCount < count || pending == nil {
            guard remaining > 0 else { throw RequestTimeout() }
            remaining -= 1
            try await Task.sleep(for: .milliseconds(10))
        }
    }

    func complete(with result: Result<PoolGamblerBet, Error>) {
        let continuation = pending
        pending = nil
        continuation?.resume(returning: result)
    }

    nonisolated func getPoolGamblerBet(
        poolId: String, gamblerId: String, matchId: String
    ) async -> Result<PoolGamblerBet, Error> {
        await withCheckedContinuation { continuation in
            Task { @MainActor in
                self.requestCount += 1
                self.pending = continuation
            }
        }
    }

    nonisolated func getPendingPoolGamblerBets(
        poolId: String, gamblerId: String, next: String?, searchText: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> { .failure(Unused()) }

    nonisolated func getFinishedPoolGamblerBets(
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
    private struct RequestTimeout: Error {}
}
