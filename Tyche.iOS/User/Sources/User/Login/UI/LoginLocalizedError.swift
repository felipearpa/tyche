import Foundation

enum LoginLocalizedError : LocalizedError {
    case invalidCredentials
    
    public var errorDescription: String? {
        switch self {
        case .invalidCredentials:
            return StringScheme.invalidCredentialFailureTitle.localizedString
        }
    }
    
    public var failureReason: String? {
        switch self {
        case .invalidCredentials:
            return StringScheme.invalidCredentialFailureMessage.localizedString
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