public protocol LoadingVisibilityDecider<Key, Item> {
    associatedtype Key
    associatedtype Item

    @MainActor
    func shouldShowLoader(lazyPagingItems: LazyPagingItems<Key, Item>) -> Bool
}
