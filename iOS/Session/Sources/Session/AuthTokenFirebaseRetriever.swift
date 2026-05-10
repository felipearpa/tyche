@preconcurrency import FirebaseAuth

final class AuthTokenFirebaseRetriever: AuthTokenRetriever {
    private let firebaseAuth: Auth
    
    init(firebaseAuth: Auth) {
        self.firebaseAuth = firebaseAuth
    }
    
    func authToken() async -> String? {
        try? await firebaseAuth.currentUser?.getIDToken()
    }
}
