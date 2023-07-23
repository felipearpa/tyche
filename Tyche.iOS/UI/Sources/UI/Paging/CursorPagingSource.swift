import Core

public typealias CursorPagingQuery<Item: Codable> = (String?) async -> Result<CursorPage<Item>, Error>

open class CursorPagingSource<TItem: Codable>: PagingSource<String, TItem> {
    public typealias TKey = String
    public typealias TItem = TItem
    
    private let pagingQuery: CursorPagingQuery<TItem>
    
    public init(pagingQuery: @escaping CursorPagingQuery<TItem>) {
        self.pagingQuery = pagingQuery
    }
    
    public override func load(loadConfig: LoadConfig<TKey>) async -> LoadResult<TKey, TItem> {
        let result = await pagingQuery(loadConfig.key)
        return result.fold(
            onSuccess: { page in
                LoadResult.page(items: page.items, nextKey: page.next)
            },
            onFailure: { error in
                LoadResult.failure(error)
            }
        )
    }
}
