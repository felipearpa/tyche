public protocol NetworkErrorHandler {
    
    func handle<T>(_ perform: () async throws -> T) async -> Result<T, Error>
}
