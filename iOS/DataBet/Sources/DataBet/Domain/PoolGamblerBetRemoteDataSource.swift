import Core

protocol PoolGamblerBetRemoteDataSource {
    func getPendingPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?
    ) async throws -> CursorPage<PoolGamblerBetResponse>

    func getFinishedPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?
    ) async throws -> CursorPage<PoolGamblerBetResponse>

    func getLivePoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?
    ) async throws -> CursorPage<PoolGamblerBetResponse>

    func getPoolMatchGamblerBets(
        poolId: String,
        matchId: String,
        next: String?
    ) async throws -> CursorPage<PoolGamblerBetResponse>

    func getGamblerBetsTimeline(
        poolId: String,
        gamblerId: String,
        next: String?
    ) async throws -> CursorPage<PoolGamblerBetResponse>

    func bet(betRequest: BetRequest) async throws -> PoolGamblerBetResponse
}
