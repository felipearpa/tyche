import Foundation

public class FirstTimeLoadingVisibilityDecider<Key, Value>: LoadingVisibilityDecider {
    private var hasShownLoading = false
    private var isContentLoaded = false

    public init() {}

    @MainActor
    public func shouldShowLoader(lazyPagingItems: LazyPagingItems<Key, Value>) -> Bool {
        if lazyPagingItems.isNotEmpty {
            return false
        }

        if lazyPagingItems.loadState.refresh.isLoading {
            hasShownLoading = true
        }

        switch lazyPagingItems.loadState.refresh {
        case .notLoading, .failure:
            if hasShownLoading {
                isContentLoaded = true
            }
        default:
            break
        }

        return lazyPagingItems.loadState.refresh.isLoading && !isContentLoaded
    }
}
