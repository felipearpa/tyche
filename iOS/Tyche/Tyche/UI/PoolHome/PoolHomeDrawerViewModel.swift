import Foundation
import Session
import DataPool
import Pool
import UI
import ViewingState

class PoolHomeDrawerViewModel: ObservableObject {
    let poolId: String
    let gamblerId: String

    private let logoutUseCase: LogOutUseCase
    private let getPoolGamblerScoreUseCase: GetPoolGamblerScoreUseCase
    private let getPoolUseCase: GetPoolUseCase
    private let deletePoolUseCase: DeletePoolUseCase
    private let accountStorage: AccountStorage

    @Published var state: LoadState<PoolGamblerScoreModel> = .idle
    @Published var email: String = ""
    @Published var username: String = ""
    @Published var isOwner: Bool = false
    @Published var deleteState: LoadState<Void> = .idle

    init(
        poolId: String,
        gamblerId: String,
        logoutUseCase: LogOutUseCase,
        getPoolGamblerScoreUseCase: GetPoolGamblerScoreUseCase,
        getPoolUseCase: GetPoolUseCase,
        deletePoolUseCase: DeletePoolUseCase,
        accountStorage: AccountStorage
    ) {
        self.poolId = poolId
        self.gamblerId = gamblerId
        self.logoutUseCase = logoutUseCase
        self.getPoolGamblerScoreUseCase = getPoolGamblerScoreUseCase
        self.getPoolUseCase = getPoolUseCase
        self.deletePoolUseCase = deletePoolUseCase
        self.accountStorage = accountStorage

        Task {
            await self.loadAccount()
            await self.loadPoolGamblerScore()
            await self.loadOwnership()
        }
    }

    @MainActor
    func loadAccount() {
        Task {
            let bundle = try? await accountStorage.retrieve()
            self.email = bundle?.email ?? ""
            self.username = bundle?.username ?? ""
        }
    }

    @MainActor
    func loadPoolGamblerScore() {
        Task {
            self.state = .loading

            let result = await getPoolGamblerScoreUseCase.execute(poolId: poolId, gamblerId: gamblerId)

            switch result {
            case .success(let score):
                self.state = .loaded(score.toPoolGamblerScoreModel())
            case .failure(let error):
                self.state = .failure(error)
            }
        }
    }

    @MainActor
    func loadOwnership() {
        Task {
            let result = await getPoolUseCase.execute(poolId: poolId)
            if case .success(let pool) = result {
                self.isOwner = pool.creatorGamblerId == gamblerId
            }
        }
    }

    @MainActor
    func signOut() {
        Task {
            let _ = await logoutUseCase.execute()
        }
    }

    @MainActor
    func deletePool(onSuccess: @escaping () -> Void) {
        Task {
            self.deleteState = .loading
            let result = await deletePoolUseCase.execute(poolId: poolId, gamblerId: gamblerId)

            switch result {
            case .success:
                self.deleteState = .loaded(())
                onSuccess()
            case .failure(let error):
                self.deleteState = .failure(error)
            }
        }
    }

    @MainActor
    func resetDeleteState() {
        deleteState = .idle
    }

    @MainActor
    func applyUsername(_ newUsername: String) {
        self.username = newUsername
    }
}
