import Foundation
import Session

enum EmailAndPasswordSignInLocalizedError: LocalizedError {
    case invalidCredentials

    public var errorDescription: String? {
        return switch self {
        case .invalidCredentials:
            String(localized: .invalidCredentialSignInFailureDescription)
        }
    }
    
    public var failureReason: String? {
        return switch self {
        case .invalidCredentials:
            String(localized: .invalidCredentialSignInFailureReason)
        }
    }
    
    public var recoverySuggestion: String? {
        return switch self {
        case .invalidCredentials:
            String(localized: .invalidCredentialSignInFailureRecoverySuggestion)
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
