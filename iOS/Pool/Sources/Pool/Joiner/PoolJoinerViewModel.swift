import Foundation
import DataPool
import UI
import ViewingState

public class PoolJoinerViewModel: ObservableObject {
    private let getPoolUseCase: GetPoolUseCase
    private let joinPoolUseCase: JoinPoolUseCase

    @Published private(set) var poolState: LoadState<PoolModel> = .idle
    @Published private(set) var joinPoolState: LoadState<Void> = .idle

    public init(getPoolUseCase: GetPoolUseCase, joinPoolUseCase: JoinPoolUseCase) {
        self.getPoolUseCase = getPoolUseCase
        self.joinPoolUseCase = joinPoolUseCase
    }

    @MainActor
    func loadPool(poolId: String) {
        Task {
            poolState = .loading
            let result = await getPoolUseCase.execute(poolId: poolId)

            switch result {
            case .success(let pool):
                poolState = .loaded(pool.toPoolModel())
            case .failure(let error):
                poolState = .failure(error)
            }
        }
    }

    @MainActor
    func joinPool(poolId: String, gamblerId: String) {
        Task {
            joinPoolState = .loading
            let result = await joinPoolUseCase.execute(
                joinPoolInput: JoinPoolInput(poolId: poolId, gamblerId: gamblerId)
            )

            switch result {
            case .success:
                joinPoolState = .loaded(())
            case .failure(let error):
                joinPoolState = .failure(error.asJoinPoolLocalizedError())
            }
        }
    }
}
