enum StatefulLazyVStackState {
    case loading
    case empty
    case error(Error)
    case content
}

extension StatefulLazyVStackState {
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
