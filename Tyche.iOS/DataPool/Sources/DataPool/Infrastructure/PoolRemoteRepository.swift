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

    func createPool(createPoolInput: CreatePoolInput) async -> Result<CreatePoolOutput, any Error> {
        return await networkErrorHandler.handle {
            let createPoolResponse = try await poolRemoteDataSource.createPool(creaePoolRequest: createPoolInput.toCreatePoolRequest())
            return createPoolResponse.toCreatePoolOutput()
        }
    }
}
