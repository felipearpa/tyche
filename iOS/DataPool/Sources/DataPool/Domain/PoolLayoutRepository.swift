import Core

public protocol PoolLayoutRepository {
    func getOpenPoolLayouts(next: String?, searchText: String?) async -> Result<CursorPage<PoolLayout>, Error>
}
