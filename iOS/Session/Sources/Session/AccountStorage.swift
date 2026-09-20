public protocol AccountStorage {
    func store(snapshot: CurrentAccountSnapshot) async throws
    func delete() async throws
    func retrieve() async throws -> CurrentAccountSnapshot?
}
