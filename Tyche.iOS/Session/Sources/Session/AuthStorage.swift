public protocol AuthStorage {
    func store(authBundle: AuthBundle) async throws
    func delete() async throws
    func retrieve() async throws -> AuthBundle?
}
