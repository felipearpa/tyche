public protocol PoolRepository {
    func getPool(id: String) async -> Result<Pool, Error>
    func createPool(createPoolInput: CreatePoolInput) async -> Result<CreatePoolOutput, Error>
    func joinPool(joinPoolInput: JoinPoolInput) async -> Result<Void, Error>
}
