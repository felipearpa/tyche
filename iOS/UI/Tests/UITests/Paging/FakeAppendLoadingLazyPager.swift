@testable import UI

class FakeAppendLoadingLazyPager: LazyPager<String, FakeItemPagingSource> {
    @MainActor
    init() {
        super.init(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: FakeFilledPagingSource()
            )
        )
        loadState = CombinedLoadStates(
            refresh: LoadState.failure(TestError()),
            append: CompleteLoadState
        )
    }
}

private struct TestError: Error {}
