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
        let finalError = mapFirebaseAuthError(error) { code in
            switch code {
            case .invalidCredential, .invalidActionCode:
                return EmailLinkSignInError.invalidEmailLink
            default:
                return nil
            }
        }
        return Result.failure(finalError)
    } catch let error {
        return Result.failure(error)
    }
}

func handleFirebaseSignInWithEmailAndPassword<Value>(_ perform: () async throws -> Value) async -> Result<Value, Error> {
    do {
        return try await Result.success(perform())
    } catch let error as NSError {
        let finalError = mapFirebaseAuthError(error) { code in
            switch code {
            case .invalidCredential, .invalidActionCode:
                return EmailAndPasswordSignInError.invalidCredentials
            default:
                return nil
            }
        }
        return Result.failure(finalError)
    } catch let error {
        return Result.failure(error)
    }
}

private func mapFirebaseAuthError(
    _ error: NSError,
    mapping: (AuthErrorCode) -> Error?
) -> Error {
    if let code = AuthErrorCode(rawValue: error.code), let mappedError = mapping(code) {
        return mappedError
    }
    return error
}
