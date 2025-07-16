public class LogOutUseCase {
    private let authenticationRepository: AuthenticationRepository
    private let accountStorage: AccountStorage
    
    init(authenticationRepository: AuthenticationRepository, accountStorage: AccountStorage) {
        self.authenticationRepository = authenticationRepository
        self.accountStorage = accountStorage
    }
    
    public func execute() async -> Result<Void, Error> {
        let logoutResult = await authenticationRepository.logOut()
        if case .success = logoutResult {
            try! await accountStorage.delete()
        }
        return logoutResult
    }
}
