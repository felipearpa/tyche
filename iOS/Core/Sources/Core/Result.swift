public extension Result {
    @inlinable
    func fold<OutValue>(
        onSuccess: (Success) -> OutValue,
        onFailure: (Failure) -> OutValue) -> OutValue
    {
        switch self {
        case .success(let value):
            return onSuccess(value)
        case .failure(let error):
            return onFailure(error)
        }
    }
    
    @inlinable
    func errorOrNil() -> Error? {
        guard case .failure(let error) = self else {
            return nil
        }
        return error
    }

    var isFailure: Bool {
        switch self {
        case .success:
            return false
        case .failure:
            return true
        }
    }
}
