import Core

protocol PoolLayoutRemoteDataSource {
    func getOpenPoolLayouts(next: String?, searchText: String?) async throws -> CursorPage<PoolLayoutResponse>
}
