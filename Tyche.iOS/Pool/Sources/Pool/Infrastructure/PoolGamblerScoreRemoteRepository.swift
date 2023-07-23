import Core

class PoolGamblerScoreRemoteRepository : PoolGamblerScoreRepository {
    private let poolGamblerScoreRemoteDataSource: PoolGamblerScoreRemoteDataSource
    
    private let networkErrorHandler: NetworkErrorHandler
    
    init(
        poolGamblerScoreRemoteDataSource: PoolGamblerScoreRemoteDataSource,
        networkErrorHandler: NetworkErrorHandler
    ) {
        self.poolGamblerScoreRemoteDataSource = poolGamblerScoreRemoteDataSource
        self.networkErrorHandler = networkErrorHandler
    }
    
    func getPoolGamblerScoresByGambler(
        gamblerId: String,
        next: String?,
        searchText: String?
    ) async -> Result<CursorPage<PoolGamblerScore>, Error> {
        return await networkErrorHandler.handle {
            let page = try await poolGamblerScoreRemoteDataSource.getPoolGamblerScoresByGambler(
                gamblerId: gamblerId,
                next: next,
                searchText: searchText
            )
            return page.map { poolGamblerScoreResponse in
                poolGamblerScoreResponse.toPoolGamblerScore()
            }
        }
    }
}
