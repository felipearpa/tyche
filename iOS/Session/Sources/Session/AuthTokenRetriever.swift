public protocol AuthTokenRetriever: Sendable {
    func authToken() async -> String?
}
