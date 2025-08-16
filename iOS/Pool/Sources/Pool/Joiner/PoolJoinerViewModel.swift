import Foundation
import DataPool
import UI

public class PoolJoinerViewModel: ObservableObject {
    private let getPoolUseCase: GetPoolUseCase
    private let joinPoolUseCase: JoinPoolUseCase

    @Published private(set) var poolState: LoadableViewState<PoolModel> = .initial
    @Published private(set) var joinPoolState: LoadableViewState<Void> = .initial

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
                poolState = .success(pool.toPoolModel())
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
                joinPoolState = .success(())
            case .failure(let error):
                joinPoolState = .failure(error)
            }
        }
    }
}
