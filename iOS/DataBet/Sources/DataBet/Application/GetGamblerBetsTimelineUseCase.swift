import Core

public class GetGamblerBetsTimelineUseCase {
    private let poolGamblerBetRepository: PoolGamblerBetRepository

    public init(poolGamblerBetRepository: PoolGamblerBetRepository) {
        self.poolGamblerBetRepository = poolGamblerBetRepository
    }

    public func execute(
        poolId: String,
        gamblerId: String,
        next: String? = nil
    ) async -> Result<CursorPage<KickoffGroup>, Error> {
        return await poolGamblerBetRepository.getGamblerBetsTimeline(
            poolId: poolId,
            gamblerId: gamblerId,
            next: next
        )
    }
}
