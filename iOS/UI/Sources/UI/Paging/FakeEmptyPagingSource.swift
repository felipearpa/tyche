import Foundation

class FakeEmptyPagingSource: PagingSource<String, FakeItemPagingSource> {
    override func load(loadConfig: LoadConfig<String>) async -> LoadResult<String, FakeItemPagingSource> {
        return LoadResult.page(
            items: [],
            nextKey: nil,
        )
    }
}
