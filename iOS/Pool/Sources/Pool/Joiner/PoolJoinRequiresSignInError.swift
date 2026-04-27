import Foundation

enum PoolJoinRequiresSignInError: LocalizedError {
    case requiresSignIn

    public var errorDescription: String? {
        return switch self {
        case .requiresSignIn:
            String(.poolJoinRequiresSignInFailureDescription)
        }
    }

    public var failureReason: String? {
        return switch self {
        case .requiresSignIn:
            String(.poolJoinRequiresSignInFailureReason)
        }
    }

    public var recoverySuggestion: String? {
        return switch self {
        case .requiresSignIn:
            String(.poolJoinRequiresSignInFailureRecoverySuggestion)
        }
    }
}
