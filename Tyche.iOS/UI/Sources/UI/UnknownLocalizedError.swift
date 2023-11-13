import Foundation

public struct UnknownLocalizedError : LocalizedError {
    
    public init() {}
    
    public var errorDescription: String? {
        return String(.unknownFailureTitle)
    }
    
    public var failureReason: String? {
        return String(.unknownFailureMessage)
    }
}
