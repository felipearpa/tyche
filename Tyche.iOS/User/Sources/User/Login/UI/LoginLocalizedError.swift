import Foundation

enum LoginLocalizedError : LocalizedError {
    case invalidCredentials
    
    public var errorDescription: String? {
        switch self {
        case .invalidCredentials:
            return String(.invalidCredentialFailureTitle)
        }
    }
    
    public var failureReason: String? {
        switch self {
        case .invalidCredentials:
            return String(.invalidCredentialFailureMessage)
        }
    }
}

extension LoginError {
    
    func toLoginLocalizedError() -> LoginLocalizedError {
        switch self {
        case .InvalidCredentials:
            return LoginLocalizedError.invalidCredentials
        }
    }
}
