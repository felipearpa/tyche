public class AccountFakeStorage: AccountStorage {
    public init() {}
    
    public func store(accountBundle: AccountBundle) async throws {}
    
    public func delete() async throws {}
    
    public func retrieve() async throws -> AccountBundle? {
        return nil
    }
}
