@preconcurrency import FirebaseAuth

final class AuthTokenFirebaseRetriever: AuthTokenRetriever {
    private let firebaseAuth: Auth
    
    init(firebaseAuth: Auth) {
        self.firebaseAuth = firebaseAuth
    }
    
    func authToken(forceRefresh: Bool) async throws -> String? {
        guard let user = firebaseAuth.currentUser else { return nil }
        return try await user.getIDToken(forcingRefresh: forceRefresh)
    }
}
