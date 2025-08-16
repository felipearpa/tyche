public protocol AccountStorage {
    func store(accountBundle: AccountBundle) async throws
    func delete() async throws
    func retrieve() async throws -> AccountBundle?
}
