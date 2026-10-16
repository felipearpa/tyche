public protocol AuthTokenRetriever: Sendable {
    /// Returns the signed-in user's ID token, or `nil` when no user is signed in.
    ///
    /// - Parameter forceRefresh: `true` fetches a new token even when the current one has not expired.
    /// - Throws: When a user is signed in but the token cannot be fetched.
    func authToken(forceRefresh: Bool) async throws -> String?
}
