public class DeletePoolUseCase {
    private let poolRepository: PoolRepository

    public init(poolRepository: PoolRepository) {
        self.poolRepository = poolRepository
    }

    public func execute(poolId: String, gamblerId: String) async -> Result<Void, Error> {
        await poolRepository.deletePool(poolId: poolId, gamblerId: gamblerId)
    }
}
