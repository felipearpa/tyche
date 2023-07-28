import Core

public class GetPoolGamblerScoresByPoolUseCase {
    private let poolGamblerScoreRepository: PoolGamblerScoreRepository
    
    public init(poolGamblerScoreRepository: PoolGamblerScoreRepository) {
        self.poolGamblerScoreRepository = poolGamblerScoreRepository
    }
    
    func execute(
        poolId: String,
        next: String? = nil,
        searchText: String? = nil
    ) async -> Result<CursorPage<PoolGamblerScore>, Error> {
        return await poolGamblerScoreRepository.getPoolGamblerScoresByPool(
            poolId: poolId,
            next: next,
            searchText: searchText
        )
    }
}
