import Foundation
@testable import UI

class FakeFilledLazyPager: LazyPagingItems<String, FakeItemPagingSource> {
    let items = (1...10).map { _ in FakeItemPagingSource(id: UUID().uuidString) }
    
    @MainActor
    init() {
        super.init(
            pagingData: Pager(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: FakeEmptyPagingSource()
            )
        )
        loadState = CombinedLoadStates(
            refresh: completeLoadState,
            append: completeLoadState
        )
    }
    
    public override func isEmpty() -> Bool {
        false
    }
    
    public override func makeIterator() -> IndexingIterator<[FakeItemPagingSource]> {
        items.makeIterator()
    }
}
