import Foundation
import Core

public extension Error {
    func orDefaultLocalized() -> LocalizedError {
        if let localizedError = self as? LocalizedError {
            return localizedError
        }
        if let networkError = self as? NetworkError {
            return networkError.toNetworkLocalizedError()
        }
        return UnknownLocalizedError()
    }

    func mapOrDefaultLocalized(_ transform: (Error) -> Error) -> LocalizedError {
        let transformedError = transform(self)
        if let localizedError = transformedError as? LocalizedError {
            return localizedError
        }
        if let networkError = transformedError as? NetworkError {
            return networkError.toNetworkLocalizedError()
        }
        return UnknownLocalizedError()
    }

    func localizedOrDefault() throws -> LocalizedError {
        if let localizedError = self as? LocalizedError {
            return localizedError
        }
        throw UnknownLocalizedError()
    }
}
