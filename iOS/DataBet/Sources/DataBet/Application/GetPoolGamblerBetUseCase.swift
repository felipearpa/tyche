import Core

public class GetPoolGamblerBetUseCase {
    private let poolGamblerBetRepository: PoolGamblerBetRepository

    public init(poolGamblerBetRepository: PoolGamblerBetRepository) {
        self.poolGamblerBetRepository = poolGamblerBetRepository
    }

    public func execute(
        poolId: String,
        gamblerId: String,
        matchId: String
    ) async -> Result<PoolGamblerBet, Error> {
        return await poolGamblerBetRepository.getPoolGamblerBet(
            poolId: poolId,
            gamblerId: gamblerId,
            matchId: matchId
        )
    }
}
