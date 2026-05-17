import Foundation
import Session

class PoolScoreListDrawerViewModel : ObservableObject {
    private let logOutUseCase: LogOutUseCase
    private let accountStorage: AccountStorage

    @Published var email: String = ""
    @Published var username: String = ""

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
