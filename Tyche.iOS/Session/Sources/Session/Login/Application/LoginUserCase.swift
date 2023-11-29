public class LoginUseCase {
    private let loginRepository: LoginRepository
    private let authStorage: AuthStorage
    private let accountStorage: AccountStorage
    
    public init(
        loginRepository: LoginRepository,
        authStorage: AuthStorage,
        accountStorage: AccountStorage
    ) {
        self.loginRepository = loginRepository
        self.authStorage = authStorage
        self.accountStorage = accountStorage
    }
    
    public func execute(loginCredential: LoginCredential) async -> Result<AccountBundle, Error> {
        let loginResult = await loginRepository.login(loginCredential: loginCredential)
        
        if case .success(let loginBundle) = loginResult {
            try! await authStorage.store(authBundle: loginBundle.authBundle)
            try! await authStorage.store(authBundle: loginBundle.authBundle)
        }
        
        return loginResult.map { loginBundle in loginBundle.accountBundle }
    }
}
