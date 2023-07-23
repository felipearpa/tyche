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
}
