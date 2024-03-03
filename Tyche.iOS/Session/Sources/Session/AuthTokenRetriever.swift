public protocol AuthTokenRetriever {
    func authToken() async -> String?
}
