import Foundation
import DataPool

enum JoinPoolLocalizedError: LocalizedError {
    case alreadyJoined

    public var errorDescription: String? {
        switch self {
        case .alreadyJoined:
            return String(localized: .poolAlreadyJoinedErrorDescription)
        }
    }

    public var failureReason: String? {
        switch self {
        case .alreadyJoined:
            return String(localized: .poolAlreadyJoinedFailureReason)
        }
    }

    public var recoverySuggestion: String? {
        switch self {
        case .alreadyJoined:
            return String(localized: .poolAlreadyJoinedRecoverySuggestion)
        }
    }
}

extension Error {
    func asJoinPoolLocalizedError() -> Error {
        if let error = self as? JoinPoolError {
            return switch error {
            case .alreadyJoined: JoinPoolLocalizedError.alreadyJoined
            }
        }
        return self
    }
}
