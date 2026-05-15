import Foundation
import Session

enum SendSignInLinkToEmailLocalizedError: LocalizedError {
    case tooManyRequests

    public var errorDescription: String? {
        switch self {
        case .tooManyRequests:
            return String(localized: .sendSignInLinkToEmailTooManyRequestsFailureDescription)
        }
    }

    public var failureReason: String? {
        switch self {
        case .tooManyRequests:
            return String(localized: .sendSignInLinkToEmailTooManyRequestsFailureReason)
        }
    }

    public var recoverySuggestion: String? {
        switch self {
        case .tooManyRequests:
            return String(localized: .sendSignInLinkToEmailTooManyRequestsFailureRecoverySuggestion)
        }
    }
}

extension Error {
    func asSendSignInLinkToEmailLocalizedError() -> Error {
        if let error = self as? SendSignInLinkToEmailError {
            return switch error {
            case .tooManyRequests: SendSignInLinkToEmailLocalizedError.tooManyRequests
            }
        }
        return self
    }
}
