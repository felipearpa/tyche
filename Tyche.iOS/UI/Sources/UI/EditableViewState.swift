public enum EditableViewState<Value> {
    case initial(Value)
    case loading(current: Value, target: Value)
    case success(current: Value, succeeded: Value)
    case failure(current: Value, failed: Value, error: Error)
}

public extension EditableViewState {
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
    func value() -> Value {
        return switch self {
        case .initial(let value), .loading(current: let value, _), .success(_, succeeded: let value), .failure(current: let value, _, _):
            value
        }
    }
    
    @inlinable
    func valueOrNull() -> Value? {
        return switch self {
        case .initial(let value), .loading(current: let value, _), .success(_, succeeded: let value):
            value
        default:
            nil
        }
    }
    
    @inlinable
    func errorOrNull() -> Error? {
        if case .failure(_, _, let error) = self {
            return error
        }
        return nil
    }
}
