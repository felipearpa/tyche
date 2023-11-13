public class LoginUseCase {
    
    private let loginRepository: LoginRepository
    
    private let loginStorage: LoginStorage
    
    public init(loginRepository: LoginRepository, loginStorage: LoginStorage) {
        self.loginRepository = loginRepository
        self.loginStorage = loginStorage
    }
    
    func execute(loginInput: LoginInput) async -> Result<UserProfile, Error> {
        let maybeLoginProfile = await loginRepository.login(loginCredential: loginInput.toLoginCrendential())
        
        if case .success(let loginProfile) = maybeLoginProfile {
            try! await loginStorage.store(loginProfile: loginProfile)
        }
        
        return maybeLoginProfile.map { loginProfile in
            loginProfile.user
        }
    }
}
