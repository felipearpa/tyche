import Foundation
import Core

public enum NetworkLocalizedError: LocalizedError {
    case remoteCommunication
    
    public var errorDescription: String? {
        switch self {
        case .remoteCommunication:
            return String(.remoteCommunicationFailureTitle)
        }
    }
    
    public var failureReason: String? {
        switch self {
        case .remoteCommunication:
            return String(.remoteCommunicationFailureMessage)
        }
    }
}

public extension NetworkError {
    func toNetworkLocalizedError() -> Error {
        return NetworkLocalizedError.remoteCommunication
    }
}
