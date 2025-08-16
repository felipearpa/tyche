import Foundation
@testable import UI

class FakeFilledLazyPager: LazyPager<String, FakeItemPagingSource> {
    let items = (1...10).map { _ in FakeItemPagingSource(id: UUID().uuidString) }
    
    @MainActor
    init() {
        super.init(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: FakeEmptyPagingSource()
            )
        )
        loadState = CombinedLoadStates(
            refresh: CompleteLoadState,
            append: CompleteLoadState
        )
    }
    
    public override func isEmpty() -> Bool {
        false
    }
    
    public override func makeIterator() -> IndexingIterator<[FakeItemPagingSource]> {
        items.makeIterator()
    }
}
