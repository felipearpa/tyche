import Foundation
import Session

enum EmailAndPasswordSignInLocalizedError: LocalizedError {
    case invalidCredentials

    public var errorDescription: String? {
        return switch self {
        case .invalidCredentials:
            String(.invalidCredentialSignInFailureDescription)
        }
    }
    
    public var failureReason: String? {
        return switch self {
        case .invalidCredentials:
            String(.invalidCredentialSignInFailureReason)
        }
    }
    
    public var recoverySuggestion: String? {
        return switch self {
        case .invalidCredentials:
            String(.invalidCredentialSignInFailureRecoverySuggestion)
        }
    }
}

extension Error {
    func asEmailAndPasswordSignInLocalizedError() -> Error {
        if let error = self as? EmailAndPasswordSignInError {
            return switch error {
            case .invalidCredentials: EmailAndPasswordSignInLocalizedError.invalidCredentials
            }
        }
        return self
    }
}
