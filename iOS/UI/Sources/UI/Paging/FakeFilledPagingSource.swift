import Foundation

class FakeFilledPagingSource: PagingSource<String, FakeItemPagingSource> {
    override func load(loadConfig: LoadConfig<String>) async -> LoadResult<String, FakeItemPagingSource> {
        return LoadResult.page(
            items: (1...10).lazy.map { _ in FakeItemPagingSource(id: UUID().uuidString) },
            nextKey: UUID().uuidString
        )
    }
}
