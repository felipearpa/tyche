import SwiftUI
import QuartzCore

public struct RefreshableStatefulLazyVStack
<Key,
 Item: Identifiable & Hashable,
 LoadingContent: View,
 RefreshLoadingContent: View,
 EmptyContent: View,
 ErrorContent: View,
 LoadingContentOnConcatenate: View,
 ErrorContentOnConcatenate: View,
 ItemView: View>: View {
    @ObservedObject var lazyPagingItems: LazyPagingItems<Key, Item>
    @State private var statefulLazyVStackState: StatefulLazyVStackState = .content
    @State private var isRefreshing: Bool = false

    let loadingContent: () -> LoadingContent
    let refreshLoadingContent: () -> RefreshLoadingContent
    let emptyContent: () -> EmptyContent
    let errorContent: (Error) -> ErrorContent
    let loadingContentOnConcatenate: () -> LoadingContentOnConcatenate
    let errorContentOnConcatenate: (Error) -> ErrorContentOnConcatenate
    let itemContent: (Item) -> ItemView
    let spacing: CGFloat
    let pinnedViews: PinnedScrollableViews
    let sectionConfiguration: SectionConfiguration<Item>?

    public init(
        lazyPagingItems: LazyPagingItems<Key, Item>,
        @ViewBuilder loadingContent: @escaping () -> LoadingContent,
        @ViewBuilder refreshLoadingContent: @escaping () -> RefreshLoadingContent,
        @ViewBuilder emptyContent: @escaping () -> EmptyContent,
        @ViewBuilder errorContent: @escaping (Error) -> ErrorContent,
        @ViewBuilder loadingContentOnConcatenate: @escaping () -> LoadingContentOnConcatenate,
        @ViewBuilder errorContentOnConcatenate: @escaping (Error) -> ErrorContentOnConcatenate,
        spacing: CGFloat = 0,
        @ViewBuilder itemContent: @escaping (Item) -> ItemView
    ) {
        self.lazyPagingItems = lazyPagingItems
        self.loadingContent = loadingContent
        self.refreshLoadingContent = refreshLoadingContent
        self.emptyContent = emptyContent
        self.errorContent = errorContent
        self.loadingContentOnConcatenate = loadingContentOnConcatenate
        self.errorContentOnConcatenate = errorContentOnConcatenate
        self.itemContent = itemContent
        self.spacing = spacing
        self.pinnedViews = []
        self.sectionConfiguration = nil
    }

    public var body: some View {
        let _ = Self._printChangesIfDebug()

        StatefulObservedLazyVStack(
            lazyPagingItems: lazyPagingItems,
            statefulLazyVStackState: $statefulLazyVStackState,
            loadingContent: loadingContent,
            refreshLoadingContent: {
                if lazyPagingItems.loadState.refresh.isLoading
                    && !statefulLazyVStackState.isLoading
                    && !isRefreshing {
                    refreshLoadingContent()
                }
            },
            emptyContent: emptyContent,
            errorContent: errorContent,
            loadingContentOnConcatenate: loadingContentOnConcatenate,
            errorContentOnConcatenate: errorContentOnConcatenate,
            pinnedViews: pinnedViews,
            sectionConfiguration: sectionConfiguration,
            spacing: spacing,
            itemContent: itemContent
        )
        .refreshable {
            isRefreshing = true
            await lazyPagingItems.refresh()
        }
        .onChange(of: lazyPagingItems.loadState.refresh) { newValue in
            if isRefreshing && lazyPagingItems.loadState.refresh.isNotLoading {
                isRefreshing = false
            }
        }
    }
}

public extension RefreshableStatefulLazyVStack
where ErrorContentOnConcatenate == StatefulLazyVStackError {
    init(
        lazyPagingItems: LazyPagingItems<Key, Item>,
        @ViewBuilder loadingContent: @escaping () -> LoadingContent,
        @ViewBuilder refreshLoadingContent: @escaping () -> RefreshLoadingContent = {
            ProgressView().progressViewStyle(.circular)
        },
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
            refreshLoadingContent: refreshLoadingContent,
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

public extension RefreshableStatefulLazyVStack
where ErrorContentOnConcatenate == StatefulLazyVStackError {
    init<SectionKey: Hashable, SectionHeader: View>(
        lazyPagingItems: LazyPagingItems<Key, Item>,
        @ViewBuilder loadingContent: @escaping () -> LoadingContent,
        @ViewBuilder refreshLoadingContent: @escaping () -> RefreshLoadingContent = {
            ProgressView().progressViewStyle(.circular)
        },
        @ViewBuilder loadingContentOnConcatenate: @escaping () -> LoadingContentOnConcatenate,
        @ViewBuilder errorContent: @escaping (Error) -> ErrorContent = { error in
            StatefulLazyVStackError(localizedError: error.localizedErrorOrDefault())
        },
        @ViewBuilder emptyContent: @escaping () -> EmptyContent = { StatefulLazyVStackEmpty() },
        spacing: CGFloat = 0,
        sectionKey: @escaping (Item) -> SectionKey,
        @ViewBuilder sectionHeader: @escaping (SectionKey) -> SectionHeader,
        @ViewBuilder itemContent: @escaping (Item) -> ItemView
    ) {
        self.lazyPagingItems = lazyPagingItems
        self.loadingContent = loadingContent
        self.refreshLoadingContent = refreshLoadingContent
        self.emptyContent = emptyContent
        self.errorContent = errorContent
        self.loadingContentOnConcatenate = loadingContentOnConcatenate
        self.errorContentOnConcatenate = { error in
            StatefulLazyVStackError(localizedError: error.localizedErrorOrDefault())
        }
        self.itemContent = itemContent
        self.spacing = spacing
        self.pinnedViews = [.sectionHeaders]
        self.sectionConfiguration = SectionConfiguration(
            key: { AnyHashable(sectionKey($0)) },
            header: { AnyView(sectionHeader($0.base as! SectionKey)) }
        )
    }
}
