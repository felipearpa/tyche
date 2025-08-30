import Foundation
import SwiftUI

public class LazyPagingItems<Key, Item: Identifiable & Hashable>: ObservableObject, Sequence {
    @Published public var loadState: CombinedLoadStates = CombinedLoadStates(
        refresh: incompleteLoadState,
        append: incompleteLoadState
    )
    private var items: [Item] = []
    private let pagingData: PagingData<Key, Item>
    private let pageFetcher: PageFetcher<Key, Item>
    private var invalidateActionId: String?
    private var retryAction: (() async -> Void)?

    public init(pagingData: PagingData<Key, Item>) {
        self.pagingData = pagingData
        self.pageFetcher = PageFetcher(pagingData: pagingData)

        Task { [pageFetcher] in
            await pageFetcher.registerInvalidateAction { [weak self] in
                guard let self else { return }
                await self.refresh()
            }
        }
    }

    deinit {
        Task { [pageFetcher, invalidateActionId] in
            if let actionId = invalidateActionId {
                await pageFetcher.unregisterInvalidateAction(actionId: actionId)
            }
        }
    }

    public func refresh() async {
        retryAction = { [weak self] in
            guard let self else { return }
            return await self.refresh()
        }

        await MainActor.run { loadState = refreshLoadingCombinedLoadState }
        let result = await pageFetcher.refresh()
        await processResult(result)
    }

    public func refreshWithoutNotification() async {
        let _ = await pageFetcher.refresh()
    }

    public func appendIfNeeded(currentIndex: Int) async {
        if isAppendNeeded(currentIndex: currentIndex) {
            retryAction = { [weak self] in
                guard let self else { return }
                return await self.appendIfNeeded(currentIndex: currentIndex)
            }

            await append()
        }
    }

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

    private func append() async {
        guard await pageFetcher.canAppend() else { return }
        guard !loadState.refresh.isLoading else { return }
        guard !loadState.append.isLoading  else { return }

        await MainActor.run { loadState = appendLoadingCombinedLoadState }
        if let result = await pageFetcher.append() {
            await processResult(result, shouldAppend: true)
        }
    }

    public func retry() async {
        guard let retryAction else { return }
        await retryAction()
    }

    private func processResult(_ result: FetchResult<Key, Item>, shouldAppend: Bool = false) async {
        switch result {
        case .page(let responsedItems, let responsedNextKey):
            await MainActor.run {
                if shouldAppend {
                    self.items.append(contentsOf: responsedItems)
                } else {
                    self.items = responsedItems
                }
                loadState = CombinedLoadStates(
                    refresh: responsedNextKey == nil ? completeLoadState : incompleteLoadState,
                    append:  responsedNextKey == nil ? completeLoadState : incompleteLoadState
                )
            }
        case .failure(let error):
            await MainActor.run {
                self.loadState = CombinedLoadStates(
                    refresh: LoadState.failure(error: error, endOfPaginationReached: false),
                    append:  completeLoadState
                )
            }
        }
    }

    public func makeIterator() -> IndexingIterator<[Item]> {
        return items.makeIterator()
    }

    public var isEmpty: Bool {
        return items.isEmpty
    }

    public var isNotEmpty: Bool {
        return !items.isEmpty
    }

    public var itemCount: Int {
        return items.count
    }
}

private let incompleteLoadState = LoadState.notLoading(endOfPaginationReached: false)
private let completeLoadState = LoadState.notLoading(endOfPaginationReached: true)

private let refreshLoadingCombinedLoadState = CombinedLoadStates(
    refresh: LoadState.loading(endOfPaginationReached: false),
    append: incompleteLoadState
)

private let appendLoadingCombinedLoadState = CombinedLoadStates(
    refresh: incompleteLoadState,
    append: LoadState.loading(endOfPaginationReached: false)
)
