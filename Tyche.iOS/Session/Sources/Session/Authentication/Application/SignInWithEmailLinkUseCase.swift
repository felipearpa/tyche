import Core

public class SignInWithEmailLinkUseCase {
    let authenticationRepository: AuthenticationRepository
    let accountStorage: AccountStorage
    
    init(authenticationRepository: AuthenticationRepository, accountStorage: AccountStorage) {
        self.authenticationRepository = authenticationRepository
        self.accountStorage = accountStorage
    }
    
    public func execute(email: Email, emailLink: String) async -> Result<AccountBundle, Error> {
        let signInResult = await authenticationRepository.signInWithEmailLink(email: email.value, emailLink: emailLink)
        guard case .success(let externalAccountId) = signInResult else {
            return Result.failure(signInResult.errorOrNil()!)
        }
        
        let linkAccountResult = await authenticationRepository.linkAccount(
            accountLink: AccountLink(email: email, externalAccountId: externalAccountId)
        )
        guard case .success(let accountBundle) = linkAccountResult else {
            return Result.failure(linkAccountResult.errorOrNil()!)
        }

        do {
            try await accountStorage.store(accountBundle: accountBundle)
        } catch {
            return Result.failure(error)
        }
        
        return Result.success(accountBundle)
    }
}
