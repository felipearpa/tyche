import Core

class PoolGamblerBetRemoteRepository: PoolGamblerBetRepository {
    private let poolGamblerBetRemoteDataSource: PoolGamblerBetRemoteDataSource
    
    private let networkErrorHandler: NetworkErrorHandler
    
    init(
        poolGamblerBetRemoteDataSource: PoolGamblerBetRemoteDataSource,
        networkErrorHandler: NetworkErrorHandler
    ) {
        self.poolGamblerBetRemoteDataSource = poolGamblerBetRemoteDataSource
        self.networkErrorHandler = networkErrorHandler
    }
    
    func getPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> {
        return await networkErrorHandler.handle {
            let page = try await poolGamblerBetRemoteDataSource.getPoolGamblerBets(
                poolId: poolId,
                gamblerId: gamblerId,
                next: next,
                searchText: searchText
            )
            return page.map { poolGamblerBetResponse in
                poolGamblerBetResponse.toPoolGamblerBet()
            }
        }
    }
    
    func bet(bet: Bet) async -> Result<PoolGamblerBet, Error> {
        return await networkErrorHandler.handle {
            let response = try await poolGamblerBetRemoteDataSource.bet(betRequest: bet.toBetRequest())
            return response.toPoolGamblerBet()
        }.recoverNetworkError { networkError in
            if case .http(let code) = networkError {
                if code == .forbidden {
                    return BetError.forbidden
                }
            }
            return networkError
        }
    }
}
