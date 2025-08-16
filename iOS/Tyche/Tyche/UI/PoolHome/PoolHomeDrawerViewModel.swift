import Foundation
import Session
import DataPool
import Pool
import UI

class PoolHomeDrawerViewModel: ObservableObject {
    let poolId: String
    let gamblerId: String

    private let logoutUseCase: LogOutUseCase
    private let getPoolGamblerScoreUseCase: GetPoolGamblerScoreUseCase

    @Published var state: LoadableViewState<PoolGamblerScoreModel> = .initial

    init(
        poolId: String,
        gamblerId: String,
        logoutUseCase: LogOutUseCase,
        getPoolGamblerScoreUseCase: GetPoolGamblerScoreUseCase
    ) {
        self.poolId = poolId
        self.gamblerId = gamblerId
        self.logoutUseCase = logoutUseCase
        self.getPoolGamblerScoreUseCase = getPoolGamblerScoreUseCase

        Task {
            await self.loadPoolGamblerScore()
        }
    }

    @MainActor
    func loadPoolGamblerScore() {
        Task {
            self.state = .loading

            let result = await getPoolGamblerScoreUseCase.execute(poolId: poolId, gamblerId: gamblerId)

            switch result {
            case .success(let score):
                self.state = .success(score.toPoolGamblerScoreModel())
            case .failure(let error):
                self.state = .failure(error)
            }
        }
    }

    @MainActor
    func signOut() {
        Task {
            let _ = await logoutUseCase.execute()
        }
    }
}
