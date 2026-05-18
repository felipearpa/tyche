import Foundation

private final class PreviewAuthenticationRepository: AuthenticationRepository {
    func sendSignInLinkToEmail(email: String) async -> Result<Void, Error> { .success(()) }
    func signInWithEmailLink(email: String, emailLink: String) async -> Result<ExternalAccountId, Error> { .success("") }
    func signInWithEmailAndPassword(email: String, password: String) async -> Result<ExternalAccountId, Error> { .success("") }
    func signInWithGoogle(idToken: String, accessToken: String) async -> Result<GoogleSignInResult, Error> {
        .success(GoogleSignInResult(externalAccountId: "", email: ""))
    }
    func logOut() async -> Result<Void, Error> { .success(()) }
    func linkAccount(accountLink: AccountLink) async -> Result<AccountBundle, Error> {
        .success(AccountBundle(accountId: "", externalAccountId: "", email: ""))
    }
    func updateUsername(accountId: String, username: String) async -> Result<Void, Error> { .success(()) }
}

public final class PreviewAccountStorage: AccountStorage {
    public init() {}
    public func store(accountBundle: AccountBundle) async throws {}
    public func delete() async throws {}
    public func retrieve() async throws -> AccountBundle? {
        AccountBundle(
            accountId: "id",
            externalAccountId: "id",
            email: "felipearpa@tyche.com",
            username: "felipearpa"
        )
    }
}

public extension LogOutUseCase {
    static func preview() -> LogOutUseCase {
        LogOutUseCase(
            authenticationRepository: PreviewAuthenticationRepository(),
            accountStorage: PreviewAccountStorage()
        )
    }
}

public extension UpdateUsernameUseCase {
    static func preview() -> UpdateUsernameUseCase {
        UpdateUsernameUseCase(
            authenticationRepository: PreviewAuthenticationRepository(),
            accountStorage: PreviewAccountStorage()
        )
    }
}
