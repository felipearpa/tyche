public protocol NetworkErrorHandler {
    func handle<Value>(_ perform: () async throws -> Value) async -> Result<Value, Error>
}
