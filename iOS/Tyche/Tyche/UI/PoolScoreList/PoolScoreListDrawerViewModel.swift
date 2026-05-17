import Foundation
import Session

class PoolScoreListDrawerViewModel : ObservableObject {
    private let logOutUseCase: LogOutUseCase
    private let updateUsernameUseCase: UpdateUsernameUseCase
    private let accountStorage: AccountStorage

    @Published var email: String = ""
    @Published var username: String = ""
    @Published var isSavingUsername: Bool = false
    @Published var usernameError: String? = nil

    static let usernameUpdateFailed = "USERNAME_UPDATE_FAILED"

    init(
        logOutUseCase: LogOutUseCase,
        updateUsernameUseCase: UpdateUsernameUseCase,
        accountStorage: AccountStorage
    ) {
        self.logOutUseCase = logOutUseCase
        self.updateUsernameUseCase = updateUsernameUseCase
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
    func changeUsername(_ newUsername: String, onSaved: @escaping () -> Void = {}) {
        guard !isSavingUsername else { return }
        let trimmed = newUsername.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty || trimmed == username {
            onSaved()
            return
        }

        Task {
            self.isSavingUsername = true
            self.usernameError = nil
            let result = await updateUsernameUseCase.execute(username: trimmed)
            switch result {
            case .success(let saved):
                self.username = saved
                onSaved()
            case .failure:
                self.usernameError = PoolScoreListDrawerViewModel.usernameUpdateFailed
            }
            self.isSavingUsername = false
        }
    }

    @MainActor
    func clearUsernameError() {
        usernameError = nil
    }
}
