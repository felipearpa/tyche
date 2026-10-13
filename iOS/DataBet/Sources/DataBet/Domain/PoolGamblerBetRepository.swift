import Core

public protocol PoolGamblerBetRepository {
    func getPendingPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error>

    /// The signed-in gambler's History: whole kickoff groups of computed matches, newest kickoff
    /// first. Fails with `KickoffGroupFeedError.cursorRejected` when the server no longer accepts
    /// `next`.
    func getFinishedPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?
    ) async -> Result<CursorPage<KickoffGroup>, Error>

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

    /// Another gambler's Timeline: whole kickoff groups, unsettled groups (earliest kickoff first)
    /// before finished ones (newest first). Fails with `KickoffGroupFeedError.cursorRejected` when
    /// the server no longer accepts `next`.
    func getGamblerBetsTimeline(
        poolId: String,
        gamblerId: String,
        next: String?
    ) async -> Result<CursorPage<KickoffGroup>, Error>

    func bet(bet: Bet) async -> Result<PoolGamblerBet, Error>
}
