import SwiftUI
import QuartzCore

public struct RefreshableStatefulLazyVStack
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
        let _ = Self._printChangesIfDebug()

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
        .refreshable {
            // Workaround: ensure the async task takes at least the given duration
            // to avoid UI race conditions when refresh completes too quickly.
            await performWithMinimumDuration(minSeconds: 1.0) {
                await lazyPagingItems.refresh()
            }
        }
    }
}

public extension RefreshableStatefulLazyVStack
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

private func performWithMinimumDuration(
    minSeconds: Double = 1.0,
    perform: @escaping () async -> Void
) async {
    let start = CACurrentMediaTime()
    await perform()
    let elapsed = CACurrentMediaTime() - start
    let remaining = max(0, minSeconds - elapsed)
    if remaining > 0 {
        try? await Task.sleep(nanoseconds: UInt64(remaining * 1_000_000_000))
    }
}
