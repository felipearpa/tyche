import Core

public class GetPoolMatchGamblerBetsUseCase {
    private let poolGamblerBetRepository: PoolGamblerBetRepository

    public init(poolGamblerBetRepository: PoolGamblerBetRepository) {
        self.poolGamblerBetRepository = poolGamblerBetRepository
    }

    public func execute(
        poolId: String,
        matchId: String,
        next: String? = nil
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> {
        return await poolGamblerBetRepository.getPoolMatchGamblerBets(
            poolId: poolId,
            matchId: matchId,
            next: next
        )
    }
}
