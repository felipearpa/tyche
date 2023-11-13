import Foundation

public struct LocalizedErrorWrapper: LocalizedError {
    var underlyingError: LocalizedError
    
    public var errorDescription: String? { underlyingError.errorDescription }
    public var failureReason: String? { underlyingError.failureReason }
    public var recoverySuggestion: String? { underlyingError.recoverySuggestion }
    public var helpAnchor: String? { underlyingError.helpAnchor }
    
    public init(underlyingError: LocalizedError) {
        self.underlyingError = underlyingError
    }
}

public extension Error {
    @inlinable
    func localizedErrorOrNull() -> LocalizedErrorWrapper? {
        if let localizedError = self as? LocalizedError {
            return LocalizedErrorWrapper(underlyingError: localizedError)
        }
        return nil
    }
}

public extension LodableViewState {
    @inlinable
    func localizedErrorOrNull() -> LocalizedErrorWrapper? {
        if case .failure(let error) = self {
            if let localizedError = error as? LocalizedError {
                return LocalizedErrorWrapper(underlyingError: localizedError)
            }
        }
        return nil
    }
}
