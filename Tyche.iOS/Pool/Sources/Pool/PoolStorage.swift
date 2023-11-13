public protocol PoolStorage {
    func store(poolProfile: PoolProfile) async throws
    
    func get() async throws -> PoolProfile?
}
