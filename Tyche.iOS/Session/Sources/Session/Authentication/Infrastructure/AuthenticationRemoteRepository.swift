import Core

class AuthenticationRemoteRepository: AuthenticationRepository {
    private let authenticationExternalDataSource: AuthenticationExternalDataSource
    private let authenticationRemoteDataSource: AuthenticationRemoteDataSource
    private let networkErrorHandler: NetworkErrorHandler
    
    init(
        authenticationExternalDataSource: AuthenticationExternalDataSource,
        authenticationRemoteDataSource: AuthenticationRemoteDataSource,
        networkErrorHandler: NetworkErrorHandler
    ) {
        self.authenticationExternalDataSource = authenticationExternalDataSource
        self.authenticationRemoteDataSource = authenticationRemoteDataSource
        self.networkErrorHandler = networkErrorHandler
    }
    
    func sendSignInLinkToEmail(email: String) async -> Result<Void, Error> {
        return await handleFirebaseSignInWithEmail {
            try await authenticationExternalDataSource.sendSignInLinkToEmail(email: email)
        }
    }
    
    func signInWithEmailLink(email: String, emailLink: String) async -> Result<ExternalAccountId, Error> {
        if await !authenticationExternalDataSource.isSignInWithEmailLink(emailLink: emailLink) {
            return Result.failure(EmailLinkSignInError.invalidEmailLink)
        }
        
        return await handleFirebaseSignInWithEmailLink {
            try await authenticationExternalDataSource.signInWithEmailLink(email: email, emailLink: emailLink)
        }
    }
    
    func logout() async -> Result<Void, Error> {
        do {
            try await authenticationExternalDataSource.signOut()
            return Result.success(())
        } catch let error {
            return Result.failure(error)
        }
    }
    
    func linkAccount(accountLink: AccountLink) async -> Result<AccountBundle, Error> {
        return await networkErrorHandler.handle {
            let linkAccountResponse = try await authenticationRemoteDataSource.linkAccount(request: accountLink.toLinkAccountRequest())
            return AccountBundle(accountId: linkAccountResponse.accountId, externalAccountId: accountLink.externalAccountId)
        }
    }
}
