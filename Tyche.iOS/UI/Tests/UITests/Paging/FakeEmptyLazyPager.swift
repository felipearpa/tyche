@testable import UI

class FakeEmptyLazyPager: LazyPager<String, FakeItemPagingSource> {
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
}
