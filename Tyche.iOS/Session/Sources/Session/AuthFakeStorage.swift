public class AuthFakeStorage: AuthStorage {
    public init() {}
    
    public func store(authBundle: AuthBundle) async throws {}
    
    public func delete() async throws {}
    
    public func retrieve() async throws -> AuthBundle? {
        return nil
    }
}
