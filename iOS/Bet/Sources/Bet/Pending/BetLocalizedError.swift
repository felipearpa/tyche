import Foundation
import DataBet

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

extension Error {
    func asBetLocalizedError() -> Error {
        if let error = self as? BetError {
            return switch error {
            case .forbidden: BetLocalizedError.forbidden
            }
        }
        return self
    }
}
