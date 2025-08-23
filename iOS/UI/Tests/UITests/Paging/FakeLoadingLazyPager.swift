@testable import UI

class FakeLoadingLazyPager: LazyPagingItems<String, FakeItemPagingSource> {
    @MainActor
    init() {
        super.init(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: FakeFilledPagingSource()
            )
        )
        loadState = refreshLoadingCombinedLoadState
    }
}
