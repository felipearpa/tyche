import Core

public class SignInWithGoogleUseCase {
    let authenticationRepository: AuthenticationRepository
    let accountStorage: AccountStorage

    init(authenticationRepository: AuthenticationRepository, accountStorage: AccountStorage) {
        self.authenticationRepository = authenticationRepository
        self.accountStorage = accountStorage
    }

    public func execute(idToken: String, accessToken: String) async -> Result<AccountBundle, Error> {
        let signInResult = await authenticationRepository.signInWithGoogle(idToken: idToken, accessToken: accessToken)
        guard case .success(let googleResult) = signInResult else {
            return Result.failure(signInResult.errorOrNil()!)
        }

        guard let email = Email(googleResult.email) else {
            return Result.failure(GoogleSignInError.invalidCredential)
        }

        let linkAccountResult = await authenticationRepository.linkAccount(
            accountLink: AccountLink(email: email, externalAccountId: googleResult.externalAccountId)
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
