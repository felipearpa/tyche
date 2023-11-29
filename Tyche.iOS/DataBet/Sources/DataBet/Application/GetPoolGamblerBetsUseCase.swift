import Core

public class GetPoolGamblerBetsUseCase {
    private let poolGamblerBetRepository: PoolGamblerBetRepository
    
    public init(poolGamblerBetRepository: PoolGamblerBetRepository) {
        self.poolGamblerBetRepository = poolGamblerBetRepository
    }
    
    public func execute(
        poolId: String,
        gamblerId: String,
        next: String? = nil,
        searchText: String? = nil
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> {
        return await poolGamblerBetRepository.getPoolGamblerBets(
            poolId: poolId, 
            gamblerId: gamblerId,
            next: next,
            searchText: searchText
        )
    }
}
