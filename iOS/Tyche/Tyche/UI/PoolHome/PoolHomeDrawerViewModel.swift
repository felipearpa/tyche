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
    private let accountStorage: AccountStorage

    @Published var state: LoadableViewState<PoolGamblerScoreModel> = .initial
    @Published var email: String = ""

    init(
        poolId: String,
        gamblerId: String,
        logoutUseCase: LogOutUseCase,
        getPoolGamblerScoreUseCase: GetPoolGamblerScoreUseCase,
        accountStorage: AccountStorage
    ) {
        self.poolId = poolId
        self.gamblerId = gamblerId
        self.logoutUseCase = logoutUseCase
        self.getPoolGamblerScoreUseCase = getPoolGamblerScoreUseCase
        self.accountStorage = accountStorage

        Task {
            await self.loadEmail()
            await self.loadPoolGamblerScore()
        }
    }

    @MainActor
    func loadEmail() {
        Task {
            self.email = (try? await accountStorage.retrieve()?.email) ?? ""
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
