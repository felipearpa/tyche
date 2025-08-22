import SwiftUI

public struct StatefulLazyVStack
<Key,
 Item: Identifiable & Hashable,
 LoadingContent: View,
 EmptyContent: View,
 ErrorContent: View,
 LoadingContentOnConcatenate: View,
 ErrorContentOnConcatenate: View,
 ItemView: View>: View
{
    @ObservedObject var lazyPagingItems: LazyPagingItems<Key, Item>
    let loadingContent: () -> LoadingContent
    let emptyContent: () -> EmptyContent
    let errorContent: (Error) -> ErrorContent
    let loadingContentOnConcatenate: () -> LoadingContentOnConcatenate
    let errorContentOnConcatenate: (Error) -> ErrorContentOnConcatenate
    let itemContent: (Item) -> ItemView
    let spacing: CGFloat
    let loadingVisibilityDecider: any LoadingVisibilityDecider<Key, Item>
    @State private var statefulState: StatefulLazyColumnState = .content

    public init(
        lazyPagingItems: LazyPagingItems<Key, Item>,
        @ViewBuilder loadingContent: @escaping () -> LoadingContent,
        @ViewBuilder emptyContent: @escaping () -> EmptyContent,
        @ViewBuilder errorContent: @escaping (Error) -> ErrorContent,
        @ViewBuilder loadingContentOnConcatenate: @escaping () -> LoadingContentOnConcatenate,
        @ViewBuilder errorContentOnConcatenate: @escaping (Error) -> ErrorContentOnConcatenate,
        spacing: CGFloat = 0,
        loadingVisibilityDecider: any LoadingVisibilityDecider<Key, Item> = FirstTimeLoadingVisibilityDecider(),
        @ViewBuilder itemContent: @escaping (Item) -> ItemView
    ) {
        self.lazyPagingItems = lazyPagingItems
        self.loadingContent = loadingContent
        self.emptyContent = emptyContent
        self.errorContent = errorContent
        self.loadingContentOnConcatenate = loadingContentOnConcatenate
        self.errorContentOnConcatenate = errorContentOnConcatenate
        self.itemContent = itemContent
        self.spacing = spacing
        self.loadingVisibilityDecider = loadingVisibilityDecider
    }

    public var body: some View {
        let shouldShowLoader = loadingVisibilityDecider.shouldShowLoader(lazyPagingItems: lazyPagingItems)
        let nextState: StatefulLazyColumnState = {
            if case .loading = lazyPagingItems.loadState.refresh, !shouldShowLoader {
                return statefulState
            } else {
                return computeStatefulLazyVStackState(shouldShowLoader: shouldShowLoader)
            }
        }()

        GeometryReader { geometryProxy in
            ScrollView {
                LazyVStack(spacing: spacing) {
                    switch nextState {
                    case .loading:
                        LoadingContentWrapper(loadingContent: loadingContent)
                    case .empty:
                        EmptyContentWrapper(geometryProxy: geometryProxy) { emptyContent() }
                    case .error(let error):
                        ErrorContentWrapper(geometryProxy: geometryProxy, error: error) { error in errorContent(error) }
                    case .content:
                        ContentWrapper(
                            lazyPager: lazyPagingItems,
                            loadingContentOnAppend: loadingContentOnConcatenate,
                            errorContentOnAppend: errorContentOnConcatenate,
                            geometryProxy: geometryProxy,
                            itemContent: itemContent,
                        )
                    }
                }
            }
            .onAppearOnce { lazyPagingItems.refresh() }
        }
    }

    private func computeStatefulLazyVStackState(shouldShowLoader: Bool) -> StatefulLazyColumnState {
        switch lazyPagingItems.loadState.refresh {
        case .failure(let error, _):
            return .error(error)
        case .notLoading:
            return lazyPagingItems.isEmpty ? .empty : .content
        case .loading:
            if shouldShowLoader { return .loading }
            return lazyPagingItems.isNotEmpty ? .content : .empty
        }
    }
}

private struct ContentWrapper
<Key,
 Item: Identifiable & Hashable,
 LoadingContentOnConcatenate: View,
 ErrorContentOnConcatenate: View,
 ItemView: View>: View
{
    @ObservedObject var lazyPager: LazyPagingItems<Key, Item>
    let loadingContentOnAppend: () -> LoadingContentOnConcatenate
    let errorContentOnAppend: (Error) -> ErrorContentOnConcatenate
    let itemContent: (Item) -> ItemView
    let geometryProxy: GeometryProxy

    init(
        lazyPager: LazyPagingItems<Key, Item>,
        loadingContentOnAppend: @escaping () -> LoadingContentOnConcatenate,
        errorContentOnAppend: @escaping (Error) -> ErrorContentOnConcatenate,
        geometryProxy: GeometryProxy,
        itemContent: @escaping (Item) -> ItemView,
    ) {
        self.lazyPager = lazyPager
        self.loadingContentOnAppend = loadingContentOnAppend
        self.errorContentOnAppend = errorContentOnAppend
        self.geometryProxy = geometryProxy
        self.itemContent = itemContent
    }

    var body: some View {
        ForEach(Array(lazyPager.enumerated()), id: \.element) { index, item in
            itemContent(item)
                .onAppearOnce {
                    lazyPager.appendIfNeeded(currentIndex: index)
                }
        }

        if case .loading = lazyPager.loadState.append {
            loadingContentOnAppend()
                .withDisableGestures()
        } else if case .failure(let error, _) = lazyPager.loadState.append {
            errorContentOnAppend(error)
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
    let geometryProxy: GeometryProxy
    let emptyContent: () -> EmptyContent

    var body: some View {
        emptyContent()
            .frame(maxWidth: .infinity, minHeight: geometryProxy.size.height)
    }
}

private struct ErrorContentWrapper<ErrorContent: View>: View {
    let geometryProxy: GeometryProxy
    let error: Error
    let errorContent: (Error) -> ErrorContent

    var body: some View {
        errorContent(error)
            .frame(maxWidth: .infinity, minHeight: geometryProxy.size.height)
    }
}

enum StatefulLazyColumnState {
    case loading
    case empty
    case error(Error)
    case content
}

public extension StatefulLazyVStack
where ErrorContentOnConcatenate == StatefulLazyVStackError {
    init(
        lazyPagingItems: LazyPagingItems<Key, Item>,
        @ViewBuilder loadingContent: @escaping () -> LoadingContent,
        @ViewBuilder loadingContentOnConcatenate: @escaping () -> LoadingContentOnConcatenate,
        @ViewBuilder errorContent: @escaping (Error) -> ErrorContent = { error in
            StatefulLazyVStackError(localizedError: error.localizedErrorOrDefault())
        },
        @ViewBuilder emptyContent: @escaping () -> EmptyContent = { StatefulLazyVStackEmpty() },
        spacing: CGFloat = 0,
        @ViewBuilder itemContent: @escaping (Item) -> ItemView,
    ) {
        self.init(
            lazyPagingItems: lazyPagingItems,
            loadingContent: loadingContent,
            emptyContent: emptyContent,
            errorContent: errorContent,
            loadingContentOnConcatenate: loadingContentOnConcatenate,
            errorContentOnConcatenate: { error in
                StatefulLazyVStackError(localizedError: error.localizedErrorOrDefault())
            },
            spacing: spacing,
            itemContent: itemContent,
        )
    }
}

private extension View {
    func withDisableGestures() -> some View {
        self.allowsHitTesting(false).scrollDisabled(true)
    }
}

#Preview {
    StatefulLazyVStack(
        lazyPagingItems: LazyPagingItems(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: { FakeFilledPagingSource() },
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
