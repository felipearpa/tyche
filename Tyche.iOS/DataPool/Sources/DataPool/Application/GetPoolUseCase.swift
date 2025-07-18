import Core

public class GetPoolUseCase {
    private let poolRepository: PoolRepository

    public init(poolRepository: PoolRepository) {
        self.poolRepository = poolRepository
    }

    public func execute(poolId: String) async -> Result<Pool, Error> {
        return await poolRepository.getPool(id: poolId)
    }
}
