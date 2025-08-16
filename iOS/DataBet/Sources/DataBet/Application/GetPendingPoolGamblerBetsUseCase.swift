import Core

public class GetPendingPoolGamblerBetsUseCase {
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
        return await poolGamblerBetRepository.getPendingPoolGamblerBets(
            poolId: poolId, 
            gamblerId: gamblerId,
            next: next,
            searchText: searchText
        )
    }
}
