@testable import UI

class FakeErrorLazyPager: LazyPagingItems<String, FakeItemPagingSource> {
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
            append: IncompleteLoadState
        )
    }
}

private struct TestError: Error {}
