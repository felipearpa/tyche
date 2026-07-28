import Foundation
import Session

class PoolScoreListDrawerViewModel : ObservableObject {
    private let logOutUseCase: LogOutUseCase
    private let accountStorage: AccountStorage

    @Published var accountId: String = ""
    @Published var email: String = ""
    @Published var username: String = ""

    var uiState: PoolScoreListDrawerUiState {
        PoolScoreListDrawerUiState(
            accountId: accountId,
            email: email,
            username: username
        )
    }

    init(
        logOutUseCase: LogOutUseCase,
        accountStorage: AccountStorage
    ) {
        self.logOutUseCase = logOutUseCase
        self.accountStorage = accountStorage

        Task { await self.loadAccount() }
    }

    @MainActor
    func loadAccount() {
        Task {
            let bundle = try? await accountStorage.retrieve()
            self.accountId = bundle?.accountId ?? ""
            self.email = bundle?.email ?? ""
            self.username = bundle?.username ?? ""
        }
    }

    @MainActor
    func logOut() {
        Task {
            await logOutUseCase.execute()
        }
    }

    @MainActor
    func applyUsername(_ newUsername: String) {
        self.username = newUsername
    }
}
