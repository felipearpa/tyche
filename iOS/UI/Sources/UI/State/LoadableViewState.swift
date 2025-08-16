public enum LoadableViewState<Value> {
    case initial
    case loading
    case success(Value)
    case failure(Error)
}

public extension LoadableViewState {
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
    func valueOrNil() -> Value? {
        if case .success(let value) = self {
            return value
        }
        return nil
    }

    @inlinable
    func errorOrNil() -> Error? {
        if case .failure(let error) = self {
            return error
        }
        return nil
    }
}

extension LoadableViewState: Equatable {
    public static func == (lhs: LoadableViewState<Value>, rhs: LoadableViewState<Value>) -> Bool {
        switch (lhs, rhs) {
        case (.initial, .initial), (.loading, .loading):
            return true

        case let (.success(l), .success(r)):
            if let lv = l as? any Equatable, let rv = r as? any Equatable {
                return String(describing: lv) == String(describing: rv)
            } else if Value.self == Void.self {
                return true
            } else {
                return false
            }

        case let (.failure(l), .failure(r)):
            return l.localizedDescription == r.localizedDescription

        default:
            return false
        }
    }
}
