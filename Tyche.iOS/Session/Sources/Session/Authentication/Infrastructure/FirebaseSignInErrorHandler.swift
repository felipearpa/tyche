import FirebaseAuth

func handleFirebaseSignInWithEmail<Value>(_ perform: () async throws -> Value) async -> Result<Value, Error> {
    do {
        return try await Result.success(perform())
    } catch let error {
        return Result.failure(error)
    }
}

func handleFirebaseSignInWithEmailLink<Value>(_ perform: () async throws -> Value) async -> Result<Value, Error> {
    do {
        return try await Result.success(perform())
    } catch let error as NSError {
        return switch AuthErrorCode(_nsError: error).code {
        case .invalidCredential, .invalidActionCode:
            Result.failure(EmailLinkSignInError.invalidEmailLink)
        default:
            Result.failure(error)
        }
    } catch let error {
        return Result.failure(error)
    }
}
