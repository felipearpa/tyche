public enum LoadState: Equatable {
    case notLoading(endOfPaginationReached: Bool)
    case loading(endOfPaginationReached: Bool)
    case failure(error: Error, endOfPaginationReached: Bool)

    var endOfPaginationReached: Bool {
        switch self {
        case .notLoading(let end), .loading(let end), .failure(_, let end):
            return end
        }
    }

    public static func ==(lhs: LoadState, rhs: LoadState) -> Bool {
        switch (lhs, rhs) {
        case (.notLoading, .notLoading):
            return true

        case (.loading, .loading):
            return true

        case (.failure, .failure):
            return true

        default:
            return false
        }
    }
}

public extension LoadState {
    var isNotLoading: Bool {
        if case .notLoading = self { return true }
        return false
    }

    var isLoading: Bool {
        if case .loading = self { return true }
        return false
    }

    var isFailure: Bool {
        if case .failure = self { return true }
        return false
    }
}
