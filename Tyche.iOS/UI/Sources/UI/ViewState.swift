public enum ViewState<T> : Equatable {
    case initial
    case loading
    case success(T)
    case failure(Error)
    
    public static func == (lhs: ViewState<T>, rhs: ViewState<T>) -> Bool {
        switch (lhs, rhs) {
        case (.initial, .initial),
            (.loading, .loading):
            return true
        case (.success, .success):
            return true
        case (.failure, .failure):
            return true
        default:
            return false
        }
    }
}

public extension ViewState {
    
    func isLoading() -> Bool {
        if case .loading = self {
            return true
        }
        return false
    }
    
    func isFailure() -> Bool {
        if case .failure = self {
            return true
        }
        return false
    }
    
    func errorOrNull() -> Error? {
        if case .failure(let error) = self {
            return error
        }
        return nil
    }
}
