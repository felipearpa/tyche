/// Forces an ID token refresh, sharing one refresh among callers that ask while it is in flight.
///
/// A refresh is shared only while it runs; a caller that arrives after it finished starts a new one.
actor ForcedTokenRefresher {
    private let authTokenRetriever: AuthTokenRetriever
    private var inFlightRefresh: Task<String?, any Error>?

    init(authTokenRetriever: AuthTokenRetriever) {
        self.authTokenRetriever = authTokenRetriever
    }

    func refresh() async throws -> String? {
        if let inFlightRefresh {
            return try await inFlightRefresh.value
        }

        let authTokenRetriever = authTokenRetriever
        let refresh = Task { try await authTokenRetriever.authToken(forceRefresh: true) }
        inFlightRefresh = refresh
        defer { inFlightRefresh = nil }
        return try await refresh.value
    }
}
