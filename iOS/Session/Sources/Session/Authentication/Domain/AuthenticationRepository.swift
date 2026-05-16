protocol AuthenticationRepository {
    func sendSignInLinkToEmail(email: String) async -> Result<Void, Error>
    func signInWithEmailLink(email: String, emailLink: String) async -> Result<ExternalAccountId, Error>
    func signInWithEmailAndPassword(email: String, password: String) async -> Result<ExternalAccountId, Error>
    func signInWithGoogle(idToken: String, accessToken: String) async -> Result<GoogleSignInResult, Error>
    func logOut() async -> Result<Void, Error>
    func linkAccount(accountLink: AccountLink) async -> Result<AccountBundle, Error>
}
