@testable import UI

class FakeLoadingLazyPager: LazyPager<String, FakeItemPagingSource> {
    @MainActor
    init() {
        super.init(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: FakeFilledPagingSource()
            )
        )
        loadState = RefreshLoading
    }
}
