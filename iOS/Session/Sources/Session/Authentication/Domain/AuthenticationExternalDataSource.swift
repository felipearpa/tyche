typealias ExternalAccountId = String

protocol AuthenticationExternalDataSource {
    func sendSignInLinkToEmail(email: String) async throws
    func signInWithEmailLink(email: String, emailLink: String) async throws -> ExternalAccountId
    func signInWithEmailAndPassword(email: String, password: String) async throws -> ExternalAccountId
    func signInWithGoogle(idToken: String, accessToken: String) async throws -> GoogleSignInResult
    func isSignInWithEmailLink(emailLink: String) async -> Bool
    func signOut() async throws
}
