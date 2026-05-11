import Foundation

public struct UnknownLocalizedError : LocalizedError {
    
    public init() {}
    
    public var errorDescription: String? {
        String(localized: .unknownFailureDescription)
    }
    
    public var failureReason: String? {
        String(localized: .unknownFailureReason)
    }
    
    public var recoverySuggestion: String? {
        String(localized: .unknownFailureRecoverySuggestion)
    }
}
