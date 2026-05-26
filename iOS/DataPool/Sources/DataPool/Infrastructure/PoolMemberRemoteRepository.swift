import Core

class PoolMemberRemoteRepository: PoolMemberRepository {
    private let poolMemberRemoteDataSource: PoolMemberRemoteDataSource

    private let networkErrorHandler: NetworkErrorHandler

    init(
        poolMemberRemoteDataSource: PoolMemberRemoteDataSource,
        networkErrorHandler: NetworkErrorHandler
    ) {
        self.poolMemberRemoteDataSource = poolMemberRemoteDataSource
        self.networkErrorHandler = networkErrorHandler
    }

    func getPoolMembers(
        poolId: String,
        next: String?
    ) async -> Result<CursorPage<PoolMember>, Error> {
        return await networkErrorHandler.handle {
            let page = try await poolMemberRemoteDataSource.getPoolMembers(
                poolId: poolId,
                next: next
            )
            return page.map { poolMemberResponse in
                poolMemberResponse.toPoolMember()
            }
        }
    }

    func removeGambler(
        poolId: String,
        gamblerId: String
    ) async -> Result<Void, Error> {
        return await networkErrorHandler.handle {
            try await poolMemberRemoteDataSource.removeGambler(
                poolId: poolId,
                gamblerId: gamblerId
            )
        }
    }
}
