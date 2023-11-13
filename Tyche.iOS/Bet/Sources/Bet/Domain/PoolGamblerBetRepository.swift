import Core

public protocol PoolGamblerBetRepository {
    func getPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error>
    
    func bet(bet: Bet) async -> Result<PoolGamblerBet, Error>
}
