import Core

class PoolRemoteRepository : PoolRepository {
    private let poolRemoteDataSource: PoolRemoteDataSource

    private let networkErrorHandler: NetworkErrorHandler

    init(
        poolRemoteDataSource: PoolRemoteDataSource,
        networkErrorHandler: NetworkErrorHandler
    ) {
        self.poolRemoteDataSource = poolRemoteDataSource
        self.networkErrorHandler = networkErrorHandler
    }

    func getPool(id: String) async -> Result<Pool, any Error> {
        return await networkErrorHandler.handle {
            let poolResponse = try await poolRemoteDataSource.getPool(id: id)
            return poolResponse.toPool()
        }
    }

    func createPool(createPoolInput: CreatePoolInput) async -> Result<CreatePoolOutput, any Error> {
        return await networkErrorHandler.handle {
            let createPoolResponse = try await poolRemoteDataSource.createPool(creaePoolRequest: createPoolInput.toCreatePoolRequest())
            return createPoolResponse.toCreatePoolOutput()
        }
    }

    func joinPool(joinPoolInput: JoinPoolInput) async -> Result<Void, any Error> {
        return await networkErrorHandler.handle {
            try await poolRemoteDataSource.joinPool(joinPoolRequest: joinPoolInput.toJoinPoolRequest())
        }
    }
}
