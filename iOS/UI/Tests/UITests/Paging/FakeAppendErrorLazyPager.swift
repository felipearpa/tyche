@testable import UI

class FakeAppendErrorLazyPager: LazyPagingItems<String, FakeItemPagingSource> {
    @MainActor
    init() {
        super.init(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: FakeFilledPagingSource()
            )
        )
        loadState = CombinedLoadStates(
            refresh: incompleteLoadState,
            append: LoadState.failure(TestError())
        )
    }
}

private struct TestError: Error {}
