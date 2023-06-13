public class LoginUseCase {
    
    private let loginRepository: LoginRepository
    
    public init(loginRepository: LoginRepository) {
        self.loginRepository = loginRepository
    }
    
    func execute(loginInput: LoginInput) async -> Result<UserProfile, Error> {
        let maybeLoginProfile = await loginRepository.login(loginCredential: loginInput.toLoginCrendential())
        return maybeLoginProfile.map { loginProfile in
            loginProfile.user
        }
    }
}
