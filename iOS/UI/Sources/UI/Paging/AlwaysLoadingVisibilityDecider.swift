import Foundation

public class AlwaysLoadingVisibilityDecider<Key, Item>: LoadingVisibilityDecider {
    public func shouldShowLoader(lazyPagingItems: LazyPagingItems<Key, Item>) -> Bool {
        return lazyPagingItems.loadState.refresh.isLoading
    }
}
