public enum LoadState {
    case notLoading(endOfPaginationReached: Bool)
    case loading(endOfPaginationReached: Bool)
    case failure(error: Error, endOfPaginationReached: Bool)

    var endOfPaginationReached: Bool {
        switch self {
        case .notLoading(let endOfPaginationReached), .loading(let endOfPaginationReached), .failure(_, let endOfPaginationReached):
            return endOfPaginationReached
        }
    }
}

extension LoadState: Equatable {
    public static func ==(lhs: LoadState, rhs: LoadState) -> Bool {
        switch (lhs, rhs) {
        case (.notLoading(let lhEndOfPaginationReached), .notLoading(let rhEndOfPaginationReached)):
            return lhEndOfPaginationReached == rhEndOfPaginationReached

        case (.loading(let lhEndOfPaginationReached), .loading(let rhEndOfPaginationReached)):
            return lhEndOfPaginationReached == rhEndOfPaginationReached

        case (.failure(let lhError, let lhEndOfPaginationReached), .failure(let rhError, let rhEndOfPaginationReached)):
            return type(of: lhError) == type(of: rhError) && lhEndOfPaginationReached == rhEndOfPaginationReached

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
