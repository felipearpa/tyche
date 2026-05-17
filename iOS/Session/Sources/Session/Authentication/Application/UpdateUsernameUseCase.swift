import Foundation

public class UpdateUsernameUseCase {
    private let authenticationRepository: AuthenticationRepository
    private let accountStorage: AccountStorage

    init(authenticationRepository: AuthenticationRepository, accountStorage: AccountStorage) {
        self.authenticationRepository = authenticationRepository
        self.accountStorage = accountStorage
    }

    public func execute(username: String) async -> Result<String, Error> {
        do {
            guard let bundle = try await accountStorage.retrieve() else {
                return .failure(UpdateUsernameError.noStoredAccount)
            }

            let result = await authenticationRepository.updateUsername(
                accountId: bundle.accountId,
                username: username
            )

            switch result {
            case .success:
                try await accountStorage.store(accountBundle: bundle.withUsername(username))
                return .success(username)
            case .failure(let error):
                return .failure(error)
            }
        } catch let error {
            return .failure(error)
        }
    }
}

public enum UpdateUsernameError: Error {
    case noStoredAccount
}
