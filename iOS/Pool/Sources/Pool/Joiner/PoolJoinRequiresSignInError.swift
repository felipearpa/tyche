import Foundation

enum PoolJoinRequiresSignInError: LocalizedError {
    case requiresSignIn

    public var errorDescription: String? {
        return switch self {
        case .requiresSignIn:
            String(localized: .poolJoinRequiresSignInFailureDescription)
        }
    }

    public var failureReason: String? {
        return switch self {
        case .requiresSignIn:
            String(localized: .poolJoinRequiresSignInFailureReason)
        }
    }

    public var recoverySuggestion: String? {
        return switch self {
        case .requiresSignIn:
            String(localized: .poolJoinRequiresSignInFailureRecoverySuggestion)
        }
    }
}
