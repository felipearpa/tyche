public struct LoadConfig<Key> {
    let key: Key?
}

public enum LoadResult<Key, Item> {
    case page(items: [Item], nextKey: Key?)
    case failure(Error)
}

open class PagingSource<Key, Item> {
    private let invalidateActionTracker = InvalidateActionTracker()
    
    func load(loadConfig: LoadConfig<Key>) async -> LoadResult<Key, Item> {
        fatalError("Not implemented")
    }
    
    func registerInvaliateAction(_ action: @escaping () -> Void) -> String {
        return invalidateActionTracker.registerAction(action)
    }
    
    func unregisterInvalidateAction(actionId: String) {
        invalidateActionTracker.unregisterAction(actionId: actionId)
    }
    
    public func invalidate() {
        invalidateActionTracker.invalidate()
    }
}
