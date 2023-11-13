extension Error {
    func toBetLocalizedError() -> Error {
        if let error = self as? BetError {
            return switch error {
            case .forbidden: BetLocalizedError.forbidden
            }
        }
        return self
    }
}
