@testable import UI

class FakeEmptyLazyPager: LazyPagingItems<String, FakeItemPagingSource> {
    @MainActor
    init() {
        super.init(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: FakeEmptyPagingSource()
            )
        )
        loadState = CombinedLoadStates(
            refresh: completeLoadState,
            append: completeLoadState
        )
    }
}
