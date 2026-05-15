import Foundation
import Core

public enum NetworkLocalizedError: LocalizedError {
    case remoteCommunication
    
    public var errorDescription: String? {
        switch self {
        case .remoteCommunication:
            return String(localized: .remoteCommunicationFailureTitle)
        }
    }
    
    public var failureReason: String? {
        switch self {
        case .remoteCommunication:
            return String(localized: .remoteCommunicationFailureMessage)
        }
    }
}

public extension NetworkError {
    func toNetworkLocalizedError() -> LocalizedError {
        return NetworkLocalizedError.remoteCommunication
    }
}
