import Combine
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
    private let currentAccountModel: CurrentAccountModel

    @Published var state: LoadState<PoolGamblerScoreModel> = .idle
    @Published var email: String = ""
    @Published var username: String = ""
    @Published var isOwner: Bool = false
    @Published var gamblerCount: Int? = nil
    @Published var deleteState: LoadState<Void> = .idle

    private var cancellables = Set<AnyCancellable>()

    var uiState: PoolHomeDrawerUiState {
        PoolHomeDrawerUiState(
            accountId: gamblerId,
            email: email,
            username: username,
            poolGamblerScoreState: state,
            isOwner: isOwner,
            gamblerCount: gamblerCount,
            isDeleting: deleteState.isLoading()
        )
    }

    init(
        poolId: String,
        gamblerId: String,
        logoutUseCase: LogOutUseCase,
        getPoolGamblerScoreUseCase: GetPoolGamblerScoreUseCase,
        getPoolUseCase: GetPoolUseCase,
        deletePoolUseCase: DeletePoolUseCase,
        currentAccountModel: CurrentAccountModel
    ) {
        self.poolId = poolId
        self.gamblerId = gamblerId
        self.logoutUseCase = logoutUseCase
        self.getPoolGamblerScoreUseCase = getPoolGamblerScoreUseCase
        self.getPoolUseCase = getPoolUseCase
        self.deletePoolUseCase = deletePoolUseCase
        self.currentAccountModel = currentAccountModel

        currentAccountModel.$account
            .sink { [weak self] account in
                self?.email = account?.email ?? ""
                self?.username = account?.username ?? ""
            }
            .store(in: &cancellables)

        Task {
            await self.loadPoolGamblerScore()
            await self.loadOwnership()
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
                self.gamblerCount = pool.gamblerCount
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
}
