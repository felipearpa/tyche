import Foundation

public class LocalizedErrorWrapper: LocalizedError {
    var underlyingError: LocalizedError

    public var errorDescription: String? { underlyingError.errorDescription }
    public var failureReason: String? { underlyingError.failureReason }
    public var recoverySuggestion: String? { underlyingError.recoverySuggestion }
    public var helpAnchor: String? { underlyingError.helpAnchor }
    
    init(underlyingError: LocalizedError) {
        self.underlyingError = underlyingError
    }
}

public extension ViewState {
    
    func localizedErrorWrapperOrNull() -> LocalizedErrorWrapper? {
        if case .failure(let error) = self {
            if let localizedError = error as? LocalizedError {
                return LocalizedErrorWrapper(underlyingError: localizedError)
            }
        }
        
        return nil
    }
}
