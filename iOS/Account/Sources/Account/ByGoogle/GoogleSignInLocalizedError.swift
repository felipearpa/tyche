import Foundation
import Session

enum GoogleSignInLocalizedError: LocalizedError {
    case invalidCredential
    case accountExistsWithDifferentCredential
    case networkError

    public var errorDescription: String? {
        return switch self {
        case .invalidCredential:
            String(localized: .googleSignInInvalidCredentialFailureDescription)
        case .accountExistsWithDifferentCredential:
            String(localized: .googleSignInAccountExistsFailureDescription)
        case .networkError:
            String(localized: .googleSignInNetworkFailureDescription)
        }
    }

    public var failureReason: String? {
        return switch self {
        case .invalidCredential:
            String(localized: .googleSignInInvalidCredentialFailureReason)
        case .accountExistsWithDifferentCredential:
            String(localized: .googleSignInAccountExistsFailureReason)
        case .networkError:
            String(localized: .googleSignInNetworkFailureReason)
        }
    }

    public var recoverySuggestion: String? {
        return switch self {
        case .invalidCredential:
            String(localized: .googleSignInInvalidCredentialFailureRecoverySuggestion)
        case .accountExistsWithDifferentCredential:
            String(localized: .googleSignInAccountExistsFailureRecoverySuggestion)
        case .networkError:
            String(localized: .googleSignInNetworkFailureRecoverySuggestion)
        }
    }
}

extension Error {
    func asGoogleSignInLocalizedError() -> Error {
        if let error = self as? GoogleSignInError {
            return switch error {
            case .invalidCredential: GoogleSignInLocalizedError.invalidCredential
            case .accountExistsWithDifferentCredential: GoogleSignInLocalizedError.accountExistsWithDifferentCredential
            case .networkError: GoogleSignInLocalizedError.networkError
            }
        }
        return self
    }
}
