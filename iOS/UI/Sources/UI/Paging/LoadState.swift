public enum LoadState: Equatable {
    case notLoading(endOfPaginationReached: Bool)
    case loading(endOfPaginationReached: Bool)
    case failure(error: Error, endOfPaginationReached: Bool)

    var endOfPaginationReached: Bool {
        switch self {
        case .notLoading(let endOfPaginationReached), .loading(let endOfPaginationReached), .failure(_, let endOfPaginationReached):
            return endOfPaginationReached
        }
    }

    public static func ==(lhs: LoadState, rhs: LoadState) -> Bool {
        switch (lhs, rhs) {
        case (.notLoading(let lhsEndOfPaginationReached), .notLoading(let rhsEndOfPaginationReached)):
            return lhsEndOfPaginationReached == rhsEndOfPaginationReached

        case (.loading(let lhsEndOfPaginationReached), .loading(let rhsEndOfPaginationReached)):
            return lhsEndOfPaginationReached == rhsEndOfPaginationReached

        case (.failure(_, let lhsEndOfPaginationReached), .failure(_, let rhsEndOfPaginationReached)):
            return lhsEndOfPaginationReached == rhsEndOfPaginationReached

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
