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

    func bet(betRequest: BetRequest) async throws -> PoolGamblerBetResponse
}
