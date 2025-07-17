public enum EditableViewState<Value> {
    case initial(Value)
    case saving(current: Value, target: Value)
    case success(old: Value, succeeded: Value)
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
    func isSaving() -> Bool {
        if case .saving = self {
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
    func relevantValue() -> Value {
        return switch self {
        case .initial(let value),
                .saving(current: let value, _),
                .success(_, succeeded: let value),
                .failure(current: let value, _, _):
            value
        }
    }
    
    @inlinable
    func errorOrNil() -> Error? {
        if case .failure(_, _, let error) = self {
            return error
        }
        return nil
    }
}
