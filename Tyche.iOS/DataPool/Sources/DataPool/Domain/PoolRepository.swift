public protocol PoolRepository {
    func createPool(createPoolInput: CreatePoolInput) async -> Result<CreatePoolOutput, Error>
}
