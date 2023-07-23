import SwiftUI

public struct PagingVStack
<Key,
 Item: Identifiable & Hashable,
 LoadingContent: View,
 EmptyContent: View,
 ErrorContent: View,
 LoadingContentOnConcatenate: View,
 ErrorContentOnConcatenate: View,
 ItemContent: View>: View
{
    @ObservedObject var lazyPager: LazyPager<Key, Item>
    let loadingContent: () -> LoadingContent
    let emptyContent: () -> EmptyContent
    let errorContent: (Error) -> ErrorContent
    let loadingContentOnConcatenate: () -> LoadingContentOnConcatenate
    let errorContentOnConcatenate: (Error) -> ErrorContentOnConcatenate
    let itemContent: (Item) -> ItemContent
    
    public init(
        lazyPager: LazyPager<Key, Item>,
        @ViewBuilder loadingContent: @escaping () -> LoadingContent,
        @ViewBuilder emptyContent: @escaping () -> EmptyContent,
        @ViewBuilder errorContent: @escaping (Error) -> ErrorContent,
        @ViewBuilder loadingContentOnConcatenate: @escaping () -> LoadingContentOnConcatenate,
        @ViewBuilder errorContentOnConcatenate: @escaping (Error) -> ErrorContentOnConcatenate,
        @ViewBuilder itemContent: @escaping (Item) -> ItemContent
    ) {
        self.lazyPager = lazyPager
        self.loadingContent = loadingContent
        self.emptyContent = emptyContent
        self.errorContent = errorContent
        self.loadingContentOnConcatenate = loadingContentOnConcatenate
        self.errorContentOnConcatenate = errorContentOnConcatenate
        self.itemContent = itemContent
    }
    
    public var body: some View {
        ZStack {
            switch lazyPager.loadState.refresh {
            case .loading:
                loadingContent()
                    .allowsHitTesting(false)
            case .notLoading(endOfPaginationReached: let endOfPaginationReached):
                MainContent(
                    lazyPager: lazyPager,
                    endOfPaginationReached: endOfPaginationReached,
                    emptyContent: emptyContent,
                    loadingContentOnAppend: loadingContentOnConcatenate,
                    errorContentOnAppend: errorContentOnConcatenate,
                    itemContent: itemContent
                )
            case .failure(let error):
                errorContent(error)
            }
        }
        .onAppearOnce {
            lazyPager.refresh()
        }
    }
}

private struct MainContent
<Key,
 Item: Identifiable & Hashable,
 EmptyContent: View,
 LoadingContentOnConcatenate: View,
 ErrorContentOnConcatenate: View,
 ItemContent: View>: View
{
    let lazyPager: LazyPager<Key, Item>
    let endOfPaginationReached: Bool
    let emptyContent: () -> EmptyContent
    let loadingContentOnAppend: () -> LoadingContentOnConcatenate
    let errorContentOnAppend: (Error) -> ErrorContentOnConcatenate
    let itemContent: (Item) -> ItemContent
    
    public var body: some View {
        if endOfPaginationReached && lazyPager.isEmpty() {
            emptyContent()
        } else {
            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach(Array(lazyPager.enumerated()), id: \.element) { index, item in
                        itemContent(item)
                            .onAppearOnce {
                                lazyPager.appendIfNeeded(currentIndex: index)
                            }
                    }
                    
                    if case .loading = lazyPager.loadState.append {
                        loadingContentOnAppend()
                            .allowsHitTesting(false)
                    } else if case .failure(let error) = lazyPager.loadState.append {
                        errorContentOnAppend(error)
                    }
                }
            }
        }
    }
}

public extension PagingVStack
where EmptyContent == PagingVStackEmpty,
      ErrorContent == PagingVStackError,
      ErrorContentOnConcatenate == PaginVStackRetryableError {
    init(
        lazyPager: LazyPager<Key, Item>,
        @ViewBuilder loadingContent: @escaping () -> LoadingContent,
        @ViewBuilder loadingContentOnConcatenate: @escaping () -> LoadingContentOnConcatenate,
        @ViewBuilder itemContent: @escaping (Item) -> ItemContent
    ) {
        self.init(
            lazyPager: lazyPager,
            loadingContent: loadingContent,
            emptyContent: { PagingVStackEmpty() },
            errorContent: { error in
                PagingVStackError(localizedError: error.localizedErrorWrapperOrNull()!)
            },
            loadingContentOnConcatenate: loadingContentOnConcatenate,
            errorContentOnConcatenate: { error in
                PaginVStackRetryableError(
                    localizedError: error.localizedErrorWrapperOrNull()!,
                    retryAction: { lazyPager.retry() }
                )
            },
            itemContent: itemContent
        )
    }
}

struct PoolScoreFakeList_Previews: PreviewProvider {
    private struct FakeItem : Identifiable, Hashable {
        let id: String
    }
    
    private class FakePagingSource: PagingSource<String, FakeItem> {
        override func load(loadConfig: LoadConfig<String>) async -> LoadResult<String, FakeItem> {
            return LoadResult.page(
                items: (1...5).lazy.map { _ in FakeItem(id: UUID().uuidString) },
                nextKey: UUID().uuidString
            )
        }
    }
    
    static var previews: some View {
        PagingVStack(
            lazyPager: LazyPager(
                pagingData: PagingData(
                    pagingConfig: PagingConfig(prefetchDistance: 5),
                    pagingSourceFactory: FakePagingSource()
                )
            ),
            loadingContent: { EmptyView() },
            emptyContent: { EmptyView() },
            errorContent: { _ in EmptyView() },
            loadingContentOnConcatenate: { EmptyView() },
            errorContentOnConcatenate: { _ in EmptyView() }
        ) { item in
            Text(item.id)
        }
    }
}
