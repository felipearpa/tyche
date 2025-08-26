import SwiftUI

public struct StatefulLazyVStack
<Key,
 Item: Identifiable & Hashable,
 LoadingContent: View,
 EmptyContent: View,
 ErrorContent: View,
 LoadingContentOnConcatenate: View,
 ErrorContentOnConcatenate: View,
 ItemView: View>: View {
    let lazyPagingItems: LazyPagingItems<Key, Item>
    @State private var statefulLazyVStackState: StatefulLazyVStackState = .content

    let loadingContent: (GeometryProxy) -> LoadingContent
    let emptyContent: (GeometryProxy) -> EmptyContent
    let errorContent: (Error, GeometryProxy) -> ErrorContent
    let loadingContentOnConcatenate: () -> LoadingContentOnConcatenate
    let errorContentOnConcatenate: (Error) -> ErrorContentOnConcatenate
    let itemContent: (Item) -> ItemView
    let spacing: CGFloat

    public init(
        lazyPagingItems: LazyPagingItems<Key, Item>,
        @ViewBuilder loadingContent: @escaping (GeometryProxy) -> LoadingContent,
        @ViewBuilder emptyContent: @escaping (GeometryProxy) -> EmptyContent,
        @ViewBuilder errorContent: @escaping (Error, GeometryProxy) -> ErrorContent,
        @ViewBuilder loadingContentOnConcatenate: @escaping () -> LoadingContentOnConcatenate,
        @ViewBuilder errorContentOnConcatenate: @escaping (Error) -> ErrorContentOnConcatenate,
        spacing: CGFloat = 0,
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
    }

    public var body: some View {
        StatefulObservedLazyVStack(
            lazyPagingItems: lazyPagingItems,
            statefulLazyVStackState: $statefulLazyVStackState,
            loadingContent: loadingContent,
            emptyContent: emptyContent,
            errorContent: errorContent,
            loadingContentOnConcatenate: loadingContentOnConcatenate,
            errorContentOnConcatenate: errorContentOnConcatenate,
            spacing: spacing,
            itemContent: itemContent
        )
    }
}

internal struct StatefulObservedLazyVStack
<Key,
 Item: Identifiable & Hashable,
 LoadingContent: View,
 EmptyContent: View,
 ErrorContent: View,
 LoadingContentOnConcatenate: View,
 ErrorContentOnConcatenate: View,
 ItemView: View>: View {
    let lazyPagingItems: LazyPagingItems<Key, Item>
    @Binding private var statefulVStackState: StatefulLazyVStackState

    let loadingContent: (GeometryProxy) -> LoadingContent
    let emptyContent: (GeometryProxy) -> EmptyContent
    let errorContent: (Error, GeometryProxy) -> ErrorContent
    let loadingContentOnConcatenate: () -> LoadingContentOnConcatenate
    let errorContentOnConcatenate: (Error) -> ErrorContentOnConcatenate
    let itemContent: (Item) -> ItemView
    let spacing: CGFloat

    public init(
        lazyPagingItems: LazyPagingItems<Key, Item>,
        statefulLazyVStackState: Binding<StatefulLazyVStackState>,
        @ViewBuilder loadingContent: @escaping (GeometryProxy) -> LoadingContent,
        @ViewBuilder emptyContent: @escaping (GeometryProxy) -> EmptyContent,
        @ViewBuilder errorContent: @escaping (Error, GeometryProxy) -> ErrorContent,
        @ViewBuilder loadingContentOnConcatenate: @escaping () -> LoadingContentOnConcatenate,
        @ViewBuilder errorContentOnConcatenate: @escaping (Error) -> ErrorContentOnConcatenate,
        spacing: CGFloat = 0,
        @ViewBuilder itemContent: @escaping (Item) -> ItemView
    ) {
        self.lazyPagingItems = lazyPagingItems
        self._statefulVStackState = .init(projectedValue: statefulLazyVStackState)
        self.loadingContent = loadingContent
        self.emptyContent = emptyContent
        self.errorContent = errorContent
        self.loadingContentOnConcatenate = loadingContentOnConcatenate
        self.errorContentOnConcatenate = errorContentOnConcatenate
        self.itemContent = itemContent
        self.spacing = spacing
    }

    public var body: some View {
        let _ = Self._printChangesIfDebug()

        GeometryReader { geometryProxy in
            ScrollView {
                LazyVStack(spacing: spacing) {
                    switch statefulVStackState {
                    case .loading:
                        loadingContent(geometryProxy)
                    case .empty:
                        emptyContent(geometryProxy)
                    case .error(let error):
                        errorContent(error, geometryProxy)
                    case .content:
                        ContentWrapper(
                            lazyPagingItems: lazyPagingItems,
                            loadingContentOnAppend: loadingContentOnConcatenate,
                            errorContentOnAppend: errorContentOnConcatenate,
                            itemContent: itemContent,
                        )
                    }
                }
                .nextStatefulLazyVStack(
                    statefulLazyVStackState: $statefulVStackState,
                    lazyPagingItems: lazyPagingItems,
                )
            }
            .task { await lazyPagingItems.refresh() }
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
    let lazyPagingItems: LazyPagingItems<Key, Item>
    let loadingContentOnAppend: () -> LoadingContentOnConcatenate
    let errorContentOnAppend: (Error) -> ErrorContentOnConcatenate
    let itemContent: (Item) -> ItemView

    init(
        lazyPagingItems: LazyPagingItems<Key, Item>,
        loadingContentOnAppend: @escaping () -> LoadingContentOnConcatenate,
        errorContentOnAppend: @escaping (Error) -> ErrorContentOnConcatenate,
        itemContent: @escaping (Item) -> ItemView,
    ) {
        self.lazyPagingItems = lazyPagingItems
        self.loadingContentOnAppend = loadingContentOnAppend
        self.errorContentOnAppend = errorContentOnAppend
        self.itemContent = itemContent
    }

    var body: some View {
        ForEach(Array(lazyPagingItems.enumerated()), id: \.element) { index, item in
            itemContent(item)
                .task { await lazyPagingItems.appendIfNeeded(currentIndex: index) }
        }

        if case .loading = lazyPagingItems.loadState.append {
            loadingContentOnAppend()
                .withDisableGestures()
        } else if case .failure(let error, _) = lazyPagingItems.loadState.append {
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
    }
}

public extension StatefulLazyVStack
where ErrorContentOnConcatenate == StatefulLazyVStackError {
    init(
        lazyPagingItems: LazyPagingItems<Key, Item>,
        @ViewBuilder loadingContent: @escaping (GeometryProxy) -> LoadingContent,
        @ViewBuilder loadingContentOnConcatenate: @escaping () -> LoadingContentOnConcatenate,
        @ViewBuilder errorContent: @escaping (Error, GeometryProxy) -> ErrorContent = { error, geometryProxy in
            StatefulLazyVStackError(localizedError: error.localizedErrorOrDefault())
                .frame(minWidth: geometryProxy.size.height)
        },
        @ViewBuilder emptyContent: @escaping (GeometryProxy) -> EmptyContent = { geometryProxy in
            StatefulLazyVStackEmpty()
                .frame(minWidth: geometryProxy.size.height)
        },
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
        loadingContent: { _ in EmptyView() },
        emptyContent: { _ in EmptyView() },
        errorContent: { _, _ in EmptyView() },
        loadingContentOnConcatenate: { EmptyView() },
        errorContentOnConcatenate: { _ in EmptyView() }
    ) { item in
        Text(item.id)
    }
}
