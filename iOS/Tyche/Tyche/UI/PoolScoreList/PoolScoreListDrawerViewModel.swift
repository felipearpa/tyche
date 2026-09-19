import Combine
import Foundation
import Session

class PoolScoreListDrawerViewModel : ObservableObject {
    private let logOutUseCase: LogOutUseCase
    private let currentAccountModel: CurrentAccountModel

    @Published var accountId: String = ""
    @Published var email: String = ""
    @Published var username: String = ""

    private var cancellables = Set<AnyCancellable>()

    var uiState: PoolScoreListDrawerUiState {
        PoolScoreListDrawerUiState(
            accountId: accountId,
            email: email,
            username: username
        )
    }

    init(
        logOutUseCase: LogOutUseCase,
        currentAccountModel: CurrentAccountModel
    ) {
        self.logOutUseCase = logOutUseCase
        self.currentAccountModel = currentAccountModel

        currentAccountModel.$account
            .sink { [weak self] account in
                self?.accountId = account?.accountId ?? ""
                self?.email = account?.email ?? ""
                self?.username = account?.username ?? ""
            }
            .store(in: &cancellables)
    }

    @MainActor
    func logOut() {
        Task {
            await logOutUseCase.execute()
        }
    }
}
