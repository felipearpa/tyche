import Core

public class GetPoolGamblerScoreUseCase {
    private let poolGamblerScoreRepository: PoolGamblerScoreRepository
    
    public init(poolGamblerScoreRepository: PoolGamblerScoreRepository) {
        self.poolGamblerScoreRepository = poolGamblerScoreRepository
    }
    
    public func execute(
        poolId: String,
        gamblerId: String
    ) async -> Result<PoolGamblerScore, Error> {
        return await poolGamblerScoreRepository.getPoolGamblerScore(
            poolId: poolId,
            gamblerId: gamblerId
        )
    }
}
