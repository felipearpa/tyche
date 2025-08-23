enum StatefulLazyVStackState: Equatable {
    case initial
    case loading
    case empty
    case error(Error)
    case content

    static func == (lhs: StatefulLazyVStackState, rhs: StatefulLazyVStackState) -> Bool {
        switch (lhs, rhs) {
        case (.initial, .initial),
            (.loading, .loading),
            (.empty, .empty),
            (.content, .content):
            return true
        case (.error, .error):
            return true
        default:
            return false
        }
    }
}

extension StatefulLazyVStackState {
    var isInitial: Bool {
        if case .initial = self { return true }
        return false
    }

    var isLoading: Bool {
        if case .loading = self { return true }
        return false
    }

    var isEmpty: Bool {
        if case .empty = self { return true }
        return false
    }

    var isError: Bool {
        if case .error = self { return true }
        return false
    }

    var isContent: Bool {
        if case .content = self { return true }
        return false
    }
}

extension StatefulLazyVStackState {
    mutating func setIfDifferent(to newValue: StatefulLazyVStackState) {
        if self != newValue {
            self = newValue
        }
    }
}
