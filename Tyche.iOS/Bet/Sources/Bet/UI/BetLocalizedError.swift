import Foundation

enum BetLocalizedError: LocalizedError {
    case forbidden
    
    public var errorDescription: String? {
        switch self {
        case .forbidden:
            return String(.forbiddenBetFailureDescription)
        }
    }
    
    public var failureReason: String? {
        switch self {
        case .forbidden:
            return String(.forbiddenBetFailureReason)
        }
    }
}
