import Foundation
import Core

public extension Error {
    func toLocalizedError() -> Error {
        if let networkError = self as? NetworkError {
            return networkError.toNetworkLocalizedError()
        }
        return UnknownLocalizedError()
    }
    
    func toLocalizedError(transformer: (Error) -> Error) -> Error {
        if let localizedError = transformer(self) as? LocalizedError {
            return localizedError
        }
        if let networkError = self as? NetworkError {
            return networkError.toNetworkLocalizedError()
        }
        return UnknownLocalizedError()
    }
}
