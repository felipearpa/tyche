import Session

extension LoginError {
    func toLoginLocalizedError() -> LoginLocalizedError {
        switch self {
        case .InvalidCredentials:
            return LoginLocalizedError.invalidCredentials
        }
    }
}
