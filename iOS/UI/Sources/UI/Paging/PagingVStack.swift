import SwiftUI

public struct PagingVStack
<Key,
 Item: Identifiable & Hashable,
 LoadingContent: View,
 EmptyContent: View,
 ErrorContent: View,
 LoadingContentOnConcatenate: View,
 ErrorContentOnConcatenate: View,
 ItemView: View>: View
{
    @ObservedObject var lazyPager: LazyPager<Key, Item>
    let loadingContent: () -> LoadingContent
    let emptyContent: () -> EmptyContent
    let errorContent: (Error) -> ErrorContent
    let loadingContentOnConcatenate: () -> LoadingContentOnConcatenate
    let errorContentOnConcatenate: (Error) -> ErrorContentOnConcatenate
    let itemContent: (Item) -> ItemView
    let spacing: CGFloat

    public init(
        lazyPager: LazyPager<Key, Item>,
        @ViewBuilder loadingContent: @escaping () -> LoadingContent,
        @ViewBuilder emptyContent: @escaping () -> EmptyContent,
        @ViewBuilder errorContent: @escaping (Error) -> ErrorContent,
        @ViewBuilder loadingContentOnConcatenate: @escaping () -> LoadingContentOnConcatenate,
        @ViewBuilder errorContentOnConcatenate: @escaping (Error) -> ErrorContentOnConcatenate,
        spacing: CGFloat = 0,
        @ViewBuilder itemContent: @escaping (Item) -> ItemView
    ) {
        self.lazyPager = lazyPager
        self.loadingContent = loadingContent
        self.emptyContent = emptyContent
        self.errorContent = errorContent
        self.loadingContentOnConcatenate = loadingContentOnConcatenate
        self.errorContentOnConcatenate = errorContentOnConcatenate
        self.itemContent = itemContent
        self.spacing = spacing
    }
    
    public var body: some View {
        switch lazyPager.loadState.refresh {
        case .loading:
            LoadingContentWrapper(loadingContent: loadingContent)
        case .notLoading(endOfPaginationReached: let endOfPaginationReached):
            ContentWrapper(
                lazyPager: lazyPager,
                endOfPaginationReached: endOfPaginationReached,
                emptyContent: emptyContent,
                loadingContentOnAppend: loadingContentOnConcatenate,
                errorContentOnAppend: errorContentOnConcatenate,
                spacing: spacing,
                itemContent: itemContent,
            )
        case .failure(let error):
            ErrorContentWrapper(errorContent: { errorContent(error) })
        }
    }
}

private struct ContentWrapper
<Key,
 Item: Identifiable & Hashable,
 EmptyContent: View,
 LoadingContentOnConcatenate: View,
 ErrorContentOnConcatenate: View,
 ItemView: View>: View
{
    @ObservedObject var lazyPager: LazyPager<Key, Item>
    let endOfPaginationReached: Bool
    let emptyContent: () -> EmptyContent
    let loadingContentOnAppend: () -> LoadingContentOnConcatenate
    let errorContentOnAppend: (Error) -> ErrorContentOnConcatenate
    let itemContent: (Item) -> ItemView
    let spacing: CGFloat

    init(
        lazyPager: LazyPager<Key, Item>,
        endOfPaginationReached: Bool,
        emptyContent: @escaping () -> EmptyContent,
        loadingContentOnAppend: @escaping () -> LoadingContentOnConcatenate,
        errorContentOnAppend: @escaping (Error) -> ErrorContentOnConcatenate,
        spacing: CGFloat = 0,
        itemContent: @escaping (Item) -> ItemView,
    ) {
        self.lazyPager = lazyPager
        self.endOfPaginationReached = endOfPaginationReached
        self.emptyContent = emptyContent
        self.loadingContentOnAppend = loadingContentOnAppend
        self.errorContentOnAppend = errorContentOnAppend
        self.itemContent = itemContent
        self.spacing = spacing
    }

    var body: some View {
        if endOfPaginationReached && lazyPager.isEmpty() {
            EmptyContentWrapper(emptyContent: emptyContent)
        } else {
            ScrollView {
                LazyVStack(spacing: spacing) {
                    ForEach(Array(lazyPager.enumerated()), id: \.element) { index, item in
                        itemContent(item)
                            .onAppearOnce {
                                lazyPager.appendIfNeeded(currentIndex: index)
                            }
                    }
                    
                    if case .loading = lazyPager.loadState.append {
                        loadingContentOnAppend()
                            .withDisableGestures()
                    } else if case .failure(let error) = lazyPager.loadState.append {
                        errorContentOnAppend(error)
                    }
                }
            }
        }
    }
}

private struct LoadingContentWrapper<LoadingContent: View>: View {
    let loadingContent: () -> LoadingContent
    
    var body: some View {
        loadingContent()
            .withDisableGestures()
    }
}

private struct EmptyContentWrapper<EmptyContent: View>: View {
    let emptyContent: () -> EmptyContent
    
    var body: some View {
        GeometryReader { geometry in
            ScrollView {
                emptyContent()
                    .withFullSize(geometry: geometry)
            }
        }
    }
}

private struct ErrorContentWrapper<ErrorContent: View>: View {
    let errorContent: () -> ErrorContent
    
    var body: some View {
        GeometryReader { geometry in
            ScrollView {
                errorContent()
                    .withFullSize(geometry: geometry)
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
        spacing: CGFloat = 0,
        @ViewBuilder itemContent: @escaping (Item) -> ItemView,
    ) {
        self.init(
            lazyPager: lazyPager,
            loadingContent: loadingContent,
            emptyContent: { PagingVStackEmpty() },
            errorContent: { error in
                PagingVStackError(localizedError: error.localizedErrorOrNil()!)
            },
            loadingContentOnConcatenate: loadingContentOnConcatenate,
            errorContentOnConcatenate: { error in
                PaginVStackRetryableError(
                    localizedError: error.localizedErrorOrNil()!,
                    retryAction: { lazyPager.retry() }
                )
            },
            spacing: spacing,
            itemContent: itemContent,
        )
    }
}

public extension PagingVStack
where ErrorContent == PagingVStackError,
      ErrorContentOnConcatenate == PaginVStackRetryableError {
    init(
        lazyPager: LazyPager<Key, Item>,
        @ViewBuilder loadingContent: @escaping () -> LoadingContent,
        @ViewBuilder loadingContentOnConcatenate: @escaping () -> LoadingContentOnConcatenate,
        @ViewBuilder emptyContent: @escaping () -> EmptyContent,
        spacing: CGFloat = 0,
        @ViewBuilder itemContent: @escaping (Item) -> ItemView,
    ) {
        self.init(
            lazyPager: lazyPager,
            loadingContent: loadingContent,
            emptyContent: emptyContent,
            errorContent: { error in
                PagingVStackError(localizedError: error.localizedErrorOrNil()!)
            },
            loadingContentOnConcatenate: loadingContentOnConcatenate,
            errorContentOnConcatenate: { error in
                PaginVStackRetryableError(
                    localizedError: error.localizedErrorOrNil()!,
                    retryAction: { lazyPager.retry() }
                )
            },
            spacing: spacing,
            itemContent: itemContent,
        )
    }
}

private extension View {
    func withDisableGestures() -> some View {
        self.allowsHitTesting(false)
    }
    
    func withFullSize(geometry: GeometryProxy) -> some View {
        self
            .frame(width: geometry.size.width)
            .frame(minHeight: geometry.size.height)
    }
}

#Preview {
    let lazyPager = LazyPager(
        pagingData: PagingData(
            pagingConfig: PagingConfig(prefetchDistance: 5),
            pagingSourceFactory: FakeFilledPagingSource(),
        )
    )

    ZStack {
        PagingVStack(
            lazyPager: lazyPager,
            loadingContent: { EmptyView() },
            emptyContent: { EmptyView() },
            errorContent: { _ in EmptyView() },
            loadingContentOnConcatenate: { EmptyView() },
            errorContentOnConcatenate: { _ in EmptyView() }
        ) { item in
            Text(item.id)
        }
    }
    .onAppear {
        lazyPager.refresh()
    }
}
