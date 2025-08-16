import Core

public class PoolLayoutRemoteRepository : PoolLayoutRepository {
    private let poolLayoutDataSource: PoolLayoutRemoteDataSource

    private let networkErrorHandler: NetworkErrorHandler

    init(poolLayoutDataSource: PoolLayoutRemoteDataSource, networkErrorHandler: NetworkErrorHandler) {
        self.poolLayoutDataSource = poolLayoutDataSource
        self.networkErrorHandler = networkErrorHandler
    }

    public func getOpenPoolLayouts(next: String?, searchText: String?) async -> Result<Core.CursorPage<PoolLayout>, any Error> {
        return await networkErrorHandler.handle {
            let page = try await poolLayoutDataSource.getOpenPoolLayouts(next: next, searchText: searchText)
            return page.map { item in item.toPoolLayout() }
        }
    }
}
