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

    /// The match's participant bets ordered by pool total (`sort=pool-score-desc`). Fails with
    /// `RankedMatchBetsError` for the ranked mode's own outcomes.
    func getRankedPoolMatchGamblerBets(
        poolId: String,
        matchId: String,
        next: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error>

    func getPoolGamblerBet(
        poolId: String,
        gamblerId: String,
        matchId: String
    ) async -> Result<PoolGamblerBet, Error>

    func getGamblerBetsTimeline(
        poolId: String,
        gamblerId: String,
        next: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error>

    func bet(bet: Bet) async -> Result<PoolGamblerBet, Error>
}
