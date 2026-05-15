import Foundation
import LazyPaging
import Core

public final class LazyPagingCursorSource<TItem: Identifiable & Hashable & Sendable & Codable>: LazyPaging.PagingSource<String, TItem>, @unchecked Sendable {
    private let pagingQuery: (String?) async -> Result<CursorPage<TItem>, Error>

    public init(pagingQuery: @escaping (String?) async -> Result<CursorPage<TItem>, Error>) {
        self.pagingQuery = pagingQuery
        super.init()
    }

    public override func load(loadConfig: LazyPaging.LoadConfig<String>) async -> LazyPaging.LoadResult<String, TItem> {
        let result = await pagingQuery(loadConfig.key)
        return result.fold(
            onSuccess: { page in
                LazyPaging.LoadResult.page(items: page.items, previousKey: nil, nextKey: page.next)
            },
            onFailure: { error in
                LazyPaging.LoadResult.failure(error)
            }
        )
    }
}
