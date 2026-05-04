import Foundation
import DataPool

enum JoinPoolLocalizedError: LocalizedError {
    case alreadyJoined

    public var errorDescription: String? {
        switch self {
        case .alreadyJoined:
            return String(.poolAlreadyJoinedErrorDescription)
        }
    }

    public var failureReason: String? {
        switch self {
        case .alreadyJoined:
            return String(.poolAlreadyJoinedFailureReason)
        }
    }

    public var recoverySuggestion: String? {
        switch self {
        case .alreadyJoined:
            return String(.poolAlreadyJoinedRecoverySuggestion)
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
