@testable import UI

class FakeAppendErrorLazyPager: LazyPager<String, FakeItemPagingSource> {
    @MainActor
    init() {
        super.init(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: FakeFilledPagingSource()
            )
        )
        loadState = CombinedLoadStates(
            refresh: IncompleteLoadState,
            append: LoadState.failure(TestError())
        )
    }
}

private struct TestError: Error {}
