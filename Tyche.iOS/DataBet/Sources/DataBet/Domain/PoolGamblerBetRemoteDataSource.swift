import Core

protocol PoolGamblerBetRemoteDataSource {
    func getPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?
    ) async throws -> CursorPage<PoolGamblerBetResponse>
    
    func bet(betRequest: BetRequest) async throws -> PoolGamblerBetResponse
}
