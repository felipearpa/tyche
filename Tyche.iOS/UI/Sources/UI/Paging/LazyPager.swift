import Foundation
import SwiftUI

private let IncompleteLoadState = LoadState.notLoading(endOfPaginationReached: false)
private let CompleteLoadState = LoadState.notLoading(endOfPaginationReached: true)

public class LazyPager<Key, Item>: ObservableObject, Sequence {
    @MainActor @Published public var loadState: CombinedLoadStates = CombinedLoadStates(
        refresh: LoadState.loading,
        append: IncompleteLoadState
    )
    
    @MainActor private var items: [Item] = []
    
    private let pagingData: PagingData<Key, Item>
    
    private var nextKey: Key? = nil
    
    private var invalidateActionId: String? = nil
    
    private var retryAction: (() async -> Void)? = nil
    
    public init(pagingData: PagingData<Key, Item>) {
        self.pagingData = pagingData
        registerInvalidateAction()
    }
    
    deinit {
        unregisterInvalidateAction()
    }
    
    private func registerInvalidateAction() {
        invalidateActionId = pagingData.pagingSourceFactory.registerInvaliateAction { [self] in
            fetch(key: pagingData.initialKey)
        }
    }
    
    private func unregisterInvalidateAction() {
        if let invalidateActionId = self.invalidateActionId {
            pagingData.pagingSourceFactory.unregisterInvalidateAction(actionId: invalidateActionId)
        }
    }
    
    private func fetch(key: Key?) {
        Task {
            await MainActor.run {
                loadState = CombinedLoadStates(
                    refresh: LoadState.loading,
                    append: IncompleteLoadState
                )
            }
            
            retryAction = { [unowned self] in fetch(key: key) }
            
            let loadResult = await pagingData.pagingSourceFactory.load(loadConfig: LoadConfig(key: key))
            
            switch loadResult {
            case .page(items: let responsedItems, nextKey: let responsedNextKey):
                nextKey = responsedNextKey
                
                await MainActor.run {
                    items = responsedItems
                    
                    loadState = CombinedLoadStates(
                        refresh: nextKey == nil ? CompleteLoadState : IncompleteLoadState,
                        append: nextKey == nil ? CompleteLoadState : IncompleteLoadState
                    )
                }
            case .failure(let responsedError):
                await MainActor.run {
                    loadState = CombinedLoadStates(
                        refresh: LoadState.failure(responsedError),
                        append: CompleteLoadState
                    )
                }
            }
        }
    }
    
    public func refresh() {
        fetch(key: pagingData.initialKey)
    }
    
    public func retry() {
        if let retryAction = self.retryAction {
            Task {
                await retryAction()
            }
        }
    }
    
    public func appendIfNeeded(currentIndex: Int) {
        Task {
            if await MainActor.run(body: { isAppendNeeded(currentIndex: currentIndex) }) {
                await append(key: nextKey)
            }
        }
    }
    
    @MainActor
    private func isAppendNeeded(currentIndex: Int) -> Bool {
        let itemsCount = items.count
        if pagingData.pagingConfig.prefetchDistance > itemsCount {
            if currentIndex == 0 {
                return true
            } else {
                return false
            }
        }
        return currentIndex == itemsCount - pagingData.pagingConfig.prefetchDistance - 1
    }
    
    private func append(key: Key?) async {
        guard key != nil else { return }
        guard await !loadState.refresh.isLoading() else { return }
        guard await !loadState.append.isLoading() else { return }
        
        await MainActor.run {
            loadState = CombinedLoadStates(
                refresh: IncompleteLoadState,
                append: LoadState.loading
            )
        }
        
        retryAction = { [unowned self] in await append(key: key) }
        
        let loadResult = await pagingData.pagingSourceFactory.load(loadConfig: LoadConfig(key: key))
        
        switch loadResult {
        case .page(items: let responsedItems, nextKey: let responsedNextKey):
            nextKey = responsedNextKey
            
            await MainActor.run {
                items.append(contentsOf: responsedItems)
                loadState = CombinedLoadStates(
                    refresh: responsedNextKey == nil ? CompleteLoadState : IncompleteLoadState,
                    append: responsedNextKey == nil ? CompleteLoadState : IncompleteLoadState
                )
            }
        case .failure(let responsedError):
            await MainActor.run {
                loadState = CombinedLoadStates(
                    refresh: LoadState.failure(responsedError),
                    append: IncompleteLoadState
                )
            }
        }
    }
    
    @MainActor
    public func makeIterator() -> IndexingIterator<[Item]> {
        return items.makeIterator()
    }
    
    @MainActor
    public func isEmpty() -> Bool {
        return items.isEmpty
    }
}

public struct CombinedLoadStates {
    public let refresh: LoadState
    public let append: LoadState
}

public enum LoadState {
    case notLoading(endOfPaginationReached: Bool)
    
    case loading
    
    case failure(Error)
}

public extension LoadState {
    @inlinable
    func isNotLoading() -> Bool {
        if case .notLoading = self {
            return true
        }
        return false
    }
    
    @inlinable
    func isLoading() -> Bool {
        if case .loading = self {
            return true
        }
        return false
    }
    
    @inlinable
    func isFailure() -> Bool {
        if case .failure = self {
            return true
        }
        return false
    }
}
