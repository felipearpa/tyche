import Core

public class SendSignInLinkToEmailUseCase {
    private let authenticationRepository: AuthenticationRepository
    
    init(authenticationRepository: AuthenticationRepository) {
        self.authenticationRepository = authenticationRepository
    }
    
    public func execute(email: Email) async -> Result<Void, Error> {
        await authenticationRepository.sendSignInLinkToEmail(email: email.value)
    }
}
