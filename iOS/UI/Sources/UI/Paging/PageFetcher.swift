import Foundation

public actor PageFetcher<Key, Item> {
    private let pagingData: PagingData<Key, Item>
    private var nextKey: Key?
    private var invalidateActionId: String?
    private var lastRefreshKey: Key?
    private var lastAppendKey: Key?
    private var pagingSource: PagingSource<Key, Item>

    public init(pagingData: PagingData<Key, Item>) {
        self.pagingData = pagingData
        self.pagingSource = pagingData.pagingSourceFactory()
    }

    public func registerInvalidateAction(_ action: @escaping () async -> Void) -> String? {
        return pagingSource.registerInvalidateAction(action)
    }

    public func unregisterInvalidateAction(actionId: String) {
        pagingSource.unregisterInvalidateAction(actionId: actionId)
    }

    public func refresh() async -> FetchResult<Key, Item> {
        let key = pagingData.initialKey
        lastRefreshKey = key
        let result = await pagingSource.load(loadConfig: LoadConfig(key: key))
        return await handle(result: result, isRefresh: true)
    }

    public func canAppend() -> Bool {
        nextKey != nil
    }

    public func append() async -> FetchResult<Key, Item>? {
        guard let key = nextKey else { return nil }
        lastAppendKey = key

        let result = await pagingSource.load(loadConfig: LoadConfig(key: key))
        return await handle(result: result, isRefresh: false)
    }

    public func currentNextKey() -> Key? {
        nextKey
    }

    private func handle(
        result: LoadResult<Key, Item>,
        isRefresh: Bool
    ) async -> FetchResult<Key, Item> {
        switch result {
        case .page(items: let items, nextKey: let newNext):
            nextKey = newNext
            return .page(items: items, nextKey: newNext)

        case .failure(let error):
            return .failure(error)
        }
    }
}

public enum FetchResult<Key, Item> {
    case page(items: [Item], nextKey: Key?)
    case failure(Error)
}
