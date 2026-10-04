import Core
import DataBet
import Foundation
import Testing
import ViewingState
@testable import Bet

/// The prediction row's save, pending, failure, retry, and cancel states, driven through the view
/// model with a repository double so that no prediction reaches a backend.
@MainActor
struct PendingBetItemViewModelTests {
    private let original = poolGamblerBetDummyModel()
    private let attempted = TeamScore(homeTeamValue: 4, awayTeamValue: 1)

    @Test
    func pendingSubmissionShowsTheAttemptedPredictionThenKeepsItOnFailure() async throws {
        let repository = ScriptedBetRepository()
        let viewModel = PendingBetItemViewModel(
            betUseCase: BetUseCase(poolGamblerBetRepository: repository)
        )
        viewModel.bind(original)

        viewModel.bet(betScore: attempted)
        let pending = try await repository.nextSubmission()

        guard case .mutating(_, let updated) = viewModel.state else {
            Issue.record("expected a pending submission, got \(String(describing: viewModel.state))")
            return
        }
        #expect(updated.betScore == attempted)
        #expect(pending.homeTeamBet.value == 4 && pending.awayTeamBet.value == 1)

        repository.complete(with: .failure(URLError(.notConnectedToInternet)))
        try await waitUntil { viewModel.state?.isFailure == true }

        guard case .failure(let failedOriginal, let failedUpdated, _) = viewModel.state else { return }
        #expect(failedOriginal == original)
        #expect(failedUpdated.betScore == attempted)
    }

    @Test
    func retrySubmitsTheSamePendingPredictionOnce() async throws {
        let repository = ScriptedBetRepository()
        let viewModel = PendingBetItemViewModel(
            betUseCase: BetUseCase(poolGamblerBetRepository: repository)
        )
        viewModel.bind(original)
        viewModel.bet(betScore: attempted)
        _ = try await repository.nextSubmission()
        repository.complete(with: .failure(URLError(.timedOut)))
        try await waitUntil { viewModel.state?.isFailure == true }

        viewModel.retryBet()
        let retried = try await repository.nextSubmission()
        #expect(retried.homeTeamBet.value == 4 && retried.awayTeamBet.value == 1)
        #expect(retried.matchId == original.matchId)

        let saved = try savedBet(from: original, betScore: attempted)
        repository.complete(with: .success(saved))
        try await waitUntil { viewModel.state?.isMutated == true }

        #expect(viewModel.state?.activeValue().betScore == attempted)
        #expect(repository.submissionCount == 2)
    }

    @Test
    func cancelAfterFailureRestoresTheSavedPrediction() async throws {
        let repository = ScriptedBetRepository()
        let viewModel = PendingBetItemViewModel(
            betUseCase: BetUseCase(poolGamblerBetRepository: repository)
        )
        viewModel.bind(original)
        viewModel.bet(betScore: attempted)
        _ = try await repository.nextSubmission()
        repository.complete(with: .failure(URLError(.timedOut)))
        try await waitUntil { viewModel.state?.isFailure == true }

        viewModel.reset()

        guard case .idle(let restored) = viewModel.state else {
            Issue.record("expected idle after cancel, got \(String(describing: viewModel.state))")
            return
        }
        #expect(restored == original)
        #expect(repository.submissionCount == 1)
    }

    /// Budgets polls, not wall time: a busy CI main actor can stall this task far past any deadline.
    private func waitUntil(
        polls: Int = 500,
        _ condition: @MainActor () -> Bool
    ) async throws {
        var remaining = polls
        while !condition() {
            guard remaining > 0 else {
                Issue.record("condition not met within \(polls) polls")
                return
            }
            remaining -= 1
            try await Task.sleep(for: .milliseconds(10))
        }
    }

    /// The repository returns the domain type, whose memberwise initializer is internal to
    /// `DataBet`; both types share their Codable shape.
    private func savedBet(
        from model: PoolGamblerBetModel,
        betScore: TeamScore<Int>
    ) throws -> PoolGamblerBet {
        let saved = model.copy { builder in builder.betScore = betScore }
        return try JSONDecoder().decode(PoolGamblerBet.self, from: JSONEncoder().encode(saved))
    }
}

private extension MutationState {
    var isFailure: Bool {
        if case .failure = self { true } else { false }
    }

    var isMutated: Bool {
        if case .mutated = self { true } else { false }
    }
}

/// Holds each submitted prediction until the test completes it, so the pending state can be
/// observed before the result arrives.
@MainActor
private final class ScriptedBetRepository: PoolGamblerBetRepository {
    private var submissions: [Bet] = []
    private var pending: CheckedContinuation<Result<PoolGamblerBet, Error>, Never>?
    private var consumed = 0

    var submissionCount: Int { submissions.count }

    /// Waits for the next submission that has not been returned yet and is awaiting a result.
    /// Budgets polls, not wall time: a busy CI main actor can stall this task far past any deadline.
    func nextSubmission(polls: Int = 500) async throws -> Bet {
        var remaining = polls
        while pending == nil || submissions.count <= consumed {
            guard remaining > 0 else { throw SubmissionTimeout() }
            remaining -= 1
            try await Task.sleep(for: .milliseconds(10))
        }
        consumed += 1
        return submissions[consumed - 1]
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

    nonisolated func getPoolGamblerBet(
        poolId: String, gamblerId: String, matchId: String
    ) async -> Result<PoolGamblerBet, Error> { .failure(Unused()) }

    nonisolated func getGamblerBetsTimeline(
        poolId: String, gamblerId: String, next: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> { .failure(Unused()) }

    private struct Unused: Error {}
    private struct SubmissionTimeout: Error {}
}
