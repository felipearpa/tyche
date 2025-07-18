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
    func localizedErrorOrNil() -> LocalizedErrorWrapper? {
        if let localizedError = self as? LocalizedError {
            return LocalizedErrorWrapper(underlyingError: localizedError)
        }
        return nil
    }

    @inlinable
    func localizedErrorOrDefault() -> LocalizedErrorWrapper {
        if let localizedError = self as? LocalizedError {
            return LocalizedErrorWrapper(underlyingError: localizedError)
        }
        return LocalizedErrorWrapper(underlyingError: UnknownLocalizedError())
    }
}

public extension LoadableViewState {
    @inlinable
    func localizedErrorOrNil() -> LocalizedErrorWrapper? {
        if case .failure(let error) = self {
            if let localizedError = error as? LocalizedError {
                return LocalizedErrorWrapper(underlyingError: localizedError)
            }
        }
        return nil
    }

    @inlinable
    func localizedErrorOrDefault() -> LocalizedErrorWrapper {
        if case .failure(let error) = self {
            if let localizedError = error as? LocalizedError {
                return LocalizedErrorWrapper(underlyingError: localizedError)
            }
        }
        return LocalizedErrorWrapper(underlyingError: UnknownLocalizedError())
    }
}
