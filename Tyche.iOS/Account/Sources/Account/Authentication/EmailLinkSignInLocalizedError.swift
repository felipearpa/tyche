import Foundation
import Session

enum EmailLinkSignInLocalizedError: LocalizedError {
    case invalidEmailLink
    
    public var errorDescription: String? {
        return switch self {
        case .invalidEmailLink:
            String(.invalidEmailLinkSignInFailureDescription)
        }
    }
    
    public var failureReason: String? {
        return switch self {
        case .invalidEmailLink:
            String(.invalidEmailLinkSignInFailureReason)
        }
    }
    
    public var recoverySuggestion: String? {
        return switch self {
        case .invalidEmailLink:
            String(.invalidEmailLinkSignInFailureRecoverySuggestion)
        }
    }
}

extension Error {
    func asEmailLinkSignInLocalizedError() -> Error {
        if let error = self as? EmailLinkSignInError {
            return switch error {
            case .invalidEmailLink: EmailLinkSignInLocalizedError.invalidEmailLink
            }
        }
        return self
    }
}
