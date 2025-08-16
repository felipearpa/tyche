protocol PoolRemoteDataSource {
    func getPool(id: String) async throws -> PoolResponse
    func createPool(creaePoolRequest: CreatePoolRequest) async throws -> CreatePoolResponse
    func joinPool(poolId: String, joinPoolRequest: JoinPoolRequest) async throws -> Void
}
