import Foundation
import Core

public enum NetworkLocalizedError: LocalizedError {
    case remoteCommunication
    
    public var errorDescription: String? {
        switch self {
        case .remoteCommunication:
            return StringScheme.remoteCommunicationFailureTitle.localizedString
        }
    }
    
    public var failureReason: String? {
        switch self {
        case .remoteCommunication:
            return StringScheme.remoteCommunicationFailureMessage.localizedString
        }
    }
}

public extension NetworkError {
    
    func toNetworkLocalizedError() -> Error {
        return NetworkLocalizedError.remoteCommunication
    }
}
