import Core

protocol PoolGamblerScoreRemoteDataSource {
    func getPoolGamblerScoresByGambler(
        gamblerId: String,
        next: String?,
        searchText: String?
    ) async throws -> CursorPage<PoolGamblerScoreResponse>
    
    func getPoolGamblerScoresByPool(
        poolId: String,
        next: String?,
        searchText: String?
    ) async throws -> CursorPage<PoolGamblerScoreResponse>
}
