import Foundation

public actor PageFetcher<Key, Item> {
    private let pagingData: PagingData<Key, Item>
    private var nextKey: Key?
    private var invalidateActionId: String?
    private var lastRefreshKey: Key?
    private var lastAppendKey: Key?
    private var retryAction: (() async -> FetchResult<Key, Item>)?
    private var pagingSource: PagingSource<Key, Item>

    public init(pagingData: PagingData<Key, Item>) {
        self.pagingData = pagingData
        self.pagingSource = pagingData.pagingSourceFactory()
    }

    public func activate() {
        registerInvalidateAction()
    }

    public func deactivate() {
        unregisterInvalidateAction()
    }

    private func registerInvalidateAction() {
        invalidateActionId = pagingSource.registerInvalidateAction { [weak self] in
            guard let self else { return }
            _ = await self.refresh()
        }
    }

    private func unregisterInvalidateAction() {
        if let id = invalidateActionId {
            pagingSource.unregisterInvalidateAction(actionId: id)
        }
    }

    public func refresh() async -> FetchResult<Key, Item> {
        let key = pagingData.initialKey
        lastRefreshKey = key

        retryAction = { [weak self] in
            guard let self else { return .failure(CancellationError()) }
            return await self.refresh()
        }

        let result = await pagingSource.load(loadConfig: LoadConfig(key: key))
        return await handle(result: result, isRefresh: true)
    }

    public func canAppend() -> Bool {
        nextKey != nil
    }

    public func append() async -> FetchResult<Key, Item> {
        guard let key = nextKey else {
            return .failure(PageFetcherError.noNextKey)
        }
        lastAppendKey = key

        retryAction = { [weak self] in
            guard let self else { return .failure(CancellationError()) }
            return await self.append()
        }

        let result = await pagingSource.load(loadConfig: LoadConfig(key: key))
        return await handle(result: result, isRefresh: false)
    }

    public func retry() async -> FetchResult<Key, Item> {
        guard let retryAction else { return .failure(PageFetcherError.noRetryAction) }
        return await retryAction()
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

public enum PageFetcherError: LocalizedError {
    case noNextKey
    case noRetryAction

    public var errorDescription: String? {
        switch self {
        case .noNextKey: return "No next key available to append."
        case .noRetryAction: return "No retry action available."
        }
    }
}

public enum FetchResult<Key, Item> {
    case page(items: [Item], nextKey: Key?)
    case failure(Error)
}
