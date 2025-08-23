import Foundation
import SwiftUI

public class LazyPagingItems<Key, Item>: ObservableObject, @preconcurrency Sequence {
    @MainActor @Published public var loadState: CombinedLoadStates = CombinedLoadStates(
        refresh: incompleteLoadState,
        append: incompleteLoadState
    )
    @MainActor private var items: [Item] = []
    private let pagingData: PagingData<Key, Item>
    private let pageFetcher: PageFetcher<Key, Item>

    public init(pagingData: PagingData<Key, Item>) {
        self.pagingData = pagingData
        self.pageFetcher = PageFetcher(pagingData: pagingData)
        Task { [pageFetcher] in await pageFetcher.activate() }
    }

    deinit {
        Task { [pageFetcher] in await pageFetcher.deactivate() }
    }

    private func fetch() {
        Task {
            await MainActor.run {
                loadState.setIfDifferent(to: refreshLoadingCombinedLoadState)
            }

            let outcome = await pageFetcher.refresh()

            switch outcome {
            case .page(let responsedItems, let responsedNextKey):
                await MainActor.run {
                    self.items = responsedItems
                    self.loadState = CombinedLoadStates(
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
    }

    public func refresh() {
        fetch()
    }

    public func appendIfNeeded(currentIndex: Int) {
        Task {
            if await MainActor.run(body: { isAppendNeeded(currentIndex: currentIndex) }) {
                await append()
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

    private func append() async {
        guard await pageFetcher.canAppend() else { return }
        guard await !loadState.refresh.isLoading else { return }
        guard await !loadState.append.isLoading  else { return }

        await MainActor.run {
            loadState.setIfDifferent(to: appendLoadingCombinedLoadState)
        }

        let outcome = await pageFetcher.append()

        switch outcome {
        case .page(let responsedItems, let responsedNextKey):
            await MainActor.run {
                self.items.append(contentsOf: responsedItems)
                self.loadState = CombinedLoadStates(
                    refresh: responsedNextKey == nil ? completeLoadState : incompleteLoadState,
                    append:  responsedNextKey == nil ? completeLoadState : incompleteLoadState
                )
            }
        case .failure(let responsedError):
            await MainActor.run {
                self.loadState = CombinedLoadStates(
                    refresh: LoadState.failure(error: responsedError, endOfPaginationReached: false),
                    append:  incompleteLoadState
                )
            }
        }
    }

    public func retry() {
        Task {
            let outcome = await pageFetcher.retry()
            switch outcome {
            case .page(let responsedItems, let responsedNextKey):
                await MainActor.run {
                    if self.items.isEmpty {
                        self.items = responsedItems
                    } else {
                        self.items.append(contentsOf: responsedItems)
                    }
                    self.loadState = CombinedLoadStates(
                        refresh: responsedNextKey == nil ? completeLoadState : incompleteLoadState,
                        append:  responsedNextKey == nil ? completeLoadState : incompleteLoadState
                    )
                }
            case .failure(let error):
                await MainActor.run {
                    self.loadState = CombinedLoadStates(
                        refresh: LoadState.failure(error: error, endOfPaginationReached: false),
                        append:  incompleteLoadState
                    )
                }
            }
        }
    }

    @MainActor
    public func makeIterator() -> IndexingIterator<[Item]> {
        return items.makeIterator()
    }

    @MainActor
    public var isEmpty: Bool {
        return items.isEmpty
    }

    @MainActor
    public var isNotEmpty: Bool {
        return !items.isEmpty
    }

    @MainActor
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
