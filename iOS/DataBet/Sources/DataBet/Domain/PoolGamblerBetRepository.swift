import Core

public protocol PoolGamblerBetRepository {
    func getPendingPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error>

    func getFinishedPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error>

    func getLivePoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error>

    func getPoolMatchGamblerBets(
        poolId: String,
        matchId: String,
        next: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error>

    func getGamblerBetsTimeline(
        poolId: String,
        gamblerId: String,
        next: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error>

    func bet(bet: Bet) async -> Result<PoolGamblerBet, Error>
}
