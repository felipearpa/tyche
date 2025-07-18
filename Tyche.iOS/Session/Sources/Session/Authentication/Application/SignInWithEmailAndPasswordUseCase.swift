import Core

public class SignInWithEmailAndPasswordUseCase {
    let authenticationRepository: AuthenticationRepository
    let accountStorage: AccountStorage
    
    init(authenticationRepository: AuthenticationRepository, accountStorage: AccountStorage) {
        self.authenticationRepository = authenticationRepository
        self.accountStorage = accountStorage
    }
    
    public func execute(email: Email, password: String) async -> Result<AccountBundle, Error> {
        let signInResult = await authenticationRepository.signInWithEmailAndPassword(email: email.value, password: password)
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
