import Foundation

private let previewAccount = AccountBundle(
    accountId: "id",
    externalAccountId: "id",
    email: "felipearpa@tyche.com",
    username: "felipearpa"
)

private final class PreviewAuthenticationRepository: AuthenticationRepository {
    private let currentAccount: AccountBundle

    init(currentAccount: AccountBundle = previewAccount) {
        self.currentAccount = currentAccount
    }

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
    func getCurrentAccount() async -> Result<AccountBundle, Error> { .success(currentAccount) }
}

public final class PreviewAccountStorage: AccountStorage {
    private let snapshot: CurrentAccountSnapshot?

    public init() {
        self.snapshot = CurrentAccountSnapshot(account: previewAccount, validatedAt: nil)
    }

    public init(snapshot: CurrentAccountSnapshot?) {
        self.snapshot = snapshot
    }

    public func store(snapshot: CurrentAccountSnapshot) async throws {}
    public func delete() async throws {}
    public func retrieve() async throws -> CurrentAccountSnapshot? { snapshot }
}

public extension CurrentAccountCoordinator {
    static func preview() -> CurrentAccountCoordinator {
        preview(account: previewAccount)
    }

    /// A coordinator hydrated with a fixed account (or none), for previews and
    /// view-model tests in targets that cannot reach Session internals.
    static func preview(account: AccountBundle?) -> CurrentAccountCoordinator {
        let coordinator = CurrentAccountCoordinator(
            accountStorage: PreviewAccountStorage(
                snapshot: account.map { CurrentAccountSnapshot(account: $0, validatedAt: nil) }
            ),
            authenticationRepository: PreviewAuthenticationRepository(currentAccount: account ?? previewAccount)
        )
        Task { await coordinator.hydrate() }
        return coordinator
    }
}

public extension CurrentAccountModel {
    static func preview() -> CurrentAccountModel {
        CurrentAccountModel(coordinator: .preview())
    }

    static func preview(account: AccountBundle?) -> CurrentAccountModel {
        CurrentAccountModel(coordinator: .preview(account: account))
    }
}

public extension LogOutUseCase {
    static func preview() -> LogOutUseCase {
        LogOutUseCase(
            authenticationRepository: PreviewAuthenticationRepository(),
            currentAccountCoordinator: .preview()
        )
    }
}

public extension UpdateUsernameUseCase {
    static func preview() -> UpdateUsernameUseCase {
        UpdateUsernameUseCase(currentAccountCoordinator: .preview())
    }
}
