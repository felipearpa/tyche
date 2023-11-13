import Core

class PoolGamblerBetFakeRepository: PoolGamblerBetRepository {
    func getPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> {
        return .success(
            CursorPage(
                items: poolGamblerBets(),
                next: nil
            )
        )
    }
    
    func bet(bet: Bet) async -> Result<PoolGamblerBet, Error> {
        return .success(poolGamblerBet())
    }
}
