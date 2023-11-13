public enum LodableViewState<Value> {
    case initial
    case loading
    case success(Value)
    case failure(Error)
}

public extension LodableViewState {
    @inlinable
    func isInitial() -> Bool {
        if case .initial = self {
            return true
        }
        return false
    }
    
    @inlinable
    func isLoading() -> Bool {
        if case .loading = self {
            return true
        }
        return false
    }
    
    @inlinable
    func isSuccess() -> Bool {
        if case .success = self {
            return true
        }
        return false
    }
    
    @inlinable
    func isFailure() -> Bool {
        if case .failure = self {
            return true
        }
        return false
    }
    
    @inlinable
    func valueOrNull() -> Value? {
        if case .success(let value) = self {
            return value
        }
        return nil
    }
    
    @inlinable
    func errorOrNull() -> Error? {
        if case .failure(let error) = self {
            return error
        }
        return nil
    }
}
