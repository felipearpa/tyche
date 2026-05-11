import Foundation
import Session

enum EmailLinkSignInLocalizedError: LocalizedError {
    case invalidEmailLink
    
    public var errorDescription: String? {
        return switch self {
        case .invalidEmailLink:
            String(localized: .invalidEmailLinkSignInFailureDescription)
        }
    }
    
    public var failureReason: String? {
        return switch self {
        case .invalidEmailLink:
            String(localized: .invalidEmailLinkSignInFailureReason)
        }
    }
    
    public var recoverySuggestion: String? {
        return switch self {
        case .invalidEmailLink:
            String(localized: .invalidEmailLinkSignInFailureRecoverySuggestion)
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
