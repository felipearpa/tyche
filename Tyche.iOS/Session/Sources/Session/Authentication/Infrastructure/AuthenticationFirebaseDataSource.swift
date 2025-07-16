import FirebaseAuth

class AuthenticationFirebaseDataSource: AuthenticationExternalDataSource {
    private let firebaseAuth: Auth
    
    init(firebaseAuth: Auth) {
        self.firebaseAuth = firebaseAuth
    }
    
    func sendSignInLinkToEmail(email: String) async throws {
        let actionCodeSettings = ActionCodeSettings()
        actionCodeSettings.url = URL(string: "https://felipearpa.github.io/tyche/signin/\(email)")
        actionCodeSettings.handleCodeInApp = true
        try await firebaseAuth.sendSignInLink(toEmail: email, actionCodeSettings: actionCodeSettings)
    }
    
    func signInWithEmailLink(email: String, emailLink: String) async throws -> ExternalAccountId {
        let authResult = try await firebaseAuth.signIn(withEmail: email, link: emailLink)
        return authResult.user.uid
    }

    func signInWithEmailAndPassword(email: String, password: String) async throws -> ExternalAccountId {
        let authResult = try await firebaseAuth.signIn(withEmail: email, password: password)
        return authResult.user.uid
    }

    func isSignInWithEmailLink(emailLink: String) async -> Bool {
        firebaseAuth.isSignIn(withEmailLink: emailLink)
    }
    
    func signOut() async throws {
        try firebaseAuth.signOut()
    }
}
