import Foundation
import DataPool
import ViewingState

/// Receives the outcomes of a `GamblerRowHolder`'s operations, so the holder does not depend on
/// the screen that keeps it.
@MainActor
protocol GamblerRowOutcomes: AnyObject {
    /// The gambler was removed from the pool.
    func removed(_ gamblerId: String)
    /// The gambler's removal failed and the row now shows the failure.
    func failed(_ gamblerId: String)
    /// The row left the failure, either by a retry (the holder is removing again) or by a
    /// dismissal (the holder is idle and has no work left).
    func failureCleared(_ gamblerId: String)
}

/// The operations of one gambler's row, kept by the screen view model so they survive the row
/// scrolling away. A started removal is never cancelled by the row; it always ends in `.mutated`
/// or `.failure` and is reported to `outcomes`.
@MainActor
final class GamblerRowHolder: ObservableObject {
    let member: PoolMemberModel

    @Published private(set) var removal: MutationState<PoolMemberModel>

    private let poolId: String
    private let removeGamblerUseCase: RemoveGamblerUseCase
    private weak var outcomes: GamblerRowOutcomes?
    private var removalTask: Task<Void, Never>?

    init(
        member: PoolMemberModel,
        poolId: String,
        removeGamblerUseCase: RemoveGamblerUseCase,
        outcomes: GamblerRowOutcomes
    ) {
        self.member = member
        self.poolId = poolId
        self.removeGamblerUseCase = removeGamblerUseCase
        self.outcomes = outcomes
        self.removal = .idle(member)
    }

    deinit {
        removalTask?.cancel()
    }

    /// Starts the removal unless one is already running or done. From a failure it restarts it.
    func remove() {
        guard !removal.isMutating(), !removal.isMutated() else { return }
        startRemoval()
    }

    /// Restarts a failed removal without asking for confirmation again.
    func retry() {
        guard removal.isFailure() else { return }
        startRemoval()
    }

    /// Returns a failed row to idle without sending a request.
    func dismissFailure() {
        guard removal.isFailure() else { return }
        removal = .idle(member)
        outcomes?.failureCleared(member.gamblerId)
    }

    private func startRemoval() {
        let wasFailure = removal.isFailure()
        removal = .mutating(member)
        if wasFailure {
            outcomes?.failureCleared(member.gamblerId)
        }

        removalTask = Task { [weak self, removeGamblerUseCase, poolId, member] in
            let result = await removeGamblerUseCase.execute(poolId: poolId, gamblerId: member.gamblerId)
            self?.finishRemoval(with: result)
        }
    }

    private func finishRemoval(with result: Result<Void, Error>) {
        switch result {
        case .success:
            removal = .mutated(member)
            outcomes?.removed(member.gamblerId)
        case .failure(let error):
            removal = .failure(value: member, error: error)
            outcomes?.failed(member.gamblerId)
        }
    }
}
