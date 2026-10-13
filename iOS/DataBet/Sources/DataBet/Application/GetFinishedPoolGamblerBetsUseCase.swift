import Core

public class GetFinishedPoolGamblerBetsUseCase {
    private let poolGamblerBetRepository: PoolGamblerBetRepository
    
    public init(poolGamblerBetRepository: PoolGamblerBetRepository) {
        self.poolGamblerBetRepository = poolGamblerBetRepository
    }
    
    public func execute(
        poolId: String,
        gamblerId: String,
        next: String? = nil
    ) async -> Result<CursorPage<KickoffGroup>, Error> {
        return await poolGamblerBetRepository.getFinishedPoolGamblerBets(
            poolId: poolId,
            gamblerId: gamblerId,
            next: next
        )
    }
}
