import Foundation

public struct UnknownLocalizedError : LocalizedError {
    
    public init() {}
    
    public var errorDescription: String? {
        String(.unknownFailureDescription)
    }
    
    public var failureReason: String? {
        String(.unknownFailureReason)
    }
    
    public var recoverySuggestion: String? {
        String(.unknownFailureRecoverySuggestion)
    }
}
