import Foundation

public struct UnknownLocalizedError : LocalizedError {
    
    public init() {}
    
    public var errorDescription: String? {
        return StringScheme.unknownFailureTitle.localizedString
    }
    
    public var failureReason: String? {
        return StringScheme.unknownFailureMessage.localizedString
    }
}
