import SwiftUI

public struct SectionConfiguration<Item> {
    let key: (Item) -> AnyHashable
    let header: (AnyHashable) -> AnyView
}

public struct StatefulLazyVStack
<Key,
 Item: Identifiable & Hashable,
 LoadingContent: View,
 RefreshLoadingContent: View,
 EmptyContent: View,
 ErrorContent: View,
 LoadingContentOnConcatenate: View,
 ErrorContentOnConcatenate: View,
 ItemView: View>: View {
    let lazyPagingItems: LazyPagingItems<Key, Item>
    @State private var statefulLazyVStackState: StatefulLazyVStackState = .content

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
        pinnedViews: PinnedScrollableViews = [],
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
        self.pinnedViews = pinnedViews
        self.sectionConfiguration = nil
    }

    public var body: some View {
        StatefulObservedLazyVStack(
            lazyPagingItems: lazyPagingItems,
            statefulLazyVStackState: $statefulLazyVStackState,
            loadingContent: loadingContent,
            refreshLoadingContent: refreshLoadingContent,
            emptyContent: emptyContent,
            errorContent: errorContent,
            loadingContentOnConcatenate: loadingContentOnConcatenate,
            errorContentOnConcatenate: errorContentOnConcatenate,
            pinnedViews: pinnedViews,
            sectionConfiguration: sectionConfiguration,
            spacing: spacing,
            itemContent: itemContent
        )
    }
}

internal struct StatefulObservedLazyVStack
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
    @Binding private var statefulLazyVStackState: StatefulLazyVStackState
    @Environment(\.boxSpacing) private var boxSpacing
    @State private var isRefreshVisible = false

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
        statefulLazyVStackState: Binding<StatefulLazyVStackState>,
        @ViewBuilder loadingContent: @escaping () -> LoadingContent,
        @ViewBuilder refreshLoadingContent: @escaping () -> RefreshLoadingContent,
        @ViewBuilder emptyContent: @escaping () -> EmptyContent,
        @ViewBuilder errorContent: @escaping (Error) -> ErrorContent,
        @ViewBuilder loadingContentOnConcatenate: @escaping () -> LoadingContentOnConcatenate,
        @ViewBuilder errorContentOnConcatenate: @escaping (Error) -> ErrorContentOnConcatenate,
        pinnedViews: PinnedScrollableViews = [],
        sectionConfiguration: SectionConfiguration<Item>? = nil,
        spacing: CGFloat = 0,
        @ViewBuilder itemContent: @escaping (Item) -> ItemView
    ) {
        self.lazyPagingItems = lazyPagingItems
        self._statefulLazyVStackState = .init(projectedValue: statefulLazyVStackState)
        self.loadingContent = loadingContent
        self.refreshLoadingContent = refreshLoadingContent
        self.emptyContent = emptyContent
        self.errorContent = errorContent
        self.loadingContentOnConcatenate = loadingContentOnConcatenate
        self.errorContentOnConcatenate = errorContentOnConcatenate
        self.itemContent = itemContent
        self.spacing = spacing
        self.pinnedViews = pinnedViews
        self.sectionConfiguration = sectionConfiguration
    }

    public var body: some View {
        let _ = Self._printChangesIfDebug()

        let shouldShowRefreshIndicator = lazyPagingItems.loadState.refresh.isLoading && !statefulLazyVStackState.isLoading

        ZStack(alignment: .top) {
            Group {
                if isRefreshVisible {
                    refreshLoadingContent()
                        .padding(boxSpacing.medium)
                        .transition(.opacity)
                }
            }
            .animation(.easeInOut, value: isRefreshVisible)

            ScrollView {
                LazyVStack(spacing: spacing, pinnedViews: pinnedViews) {
                    switch statefulLazyVStackState {
                    case .loading:
                        loadingContent()
                    case .empty:
                        emptyContent()
                    case .error(let error):
                        errorContent(error)
                    case .content:
                        if let sectionConfiguration {
                            SectionedContentWrapper(
                                lazyPagingItems: lazyPagingItems,
                                sectionConfiguration: sectionConfiguration,
                                loadingContentOnAppend: loadingContentOnConcatenate,
                                errorContentOnAppend: errorContentOnConcatenate,
                                itemContent: itemContent
                            )
                        } else {
                            ContentWrapper(
                                lazyPagingItems: lazyPagingItems,
                                loadingContentOnAppend: loadingContentOnConcatenate,
                                errorContentOnAppend: errorContentOnConcatenate,
                                itemContent: itemContent
                            )
                        }
                    }
                }
                .nextStatefulLazyVStack(
                    statefulLazyVStackState: $statefulLazyVStackState,
                    lazyPagingItems: lazyPagingItems
                )
            }
            .withDisableGestures(when: statefulLazyVStackState.isLoading)
        }
        .task { await lazyPagingItems.refresh() }
        .onChange(of: shouldShowRefreshIndicator) { newShouldShowRefreshIndicator in
            withAnimation { isRefreshVisible = newShouldShowRefreshIndicator }
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
    @ObservedObject var lazyPagingItems: LazyPagingItems<Key, Item>
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
        ForEach(Array(lazyPagingItems.enumerated()), id: \.element.id) { index, item in
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

private struct ItemSection<Item>: Identifiable {
    var id: AnyHashable { key }
    let key: AnyHashable
    let items: [(offset: Int, element: Item)]
}

private struct SectionedContentWrapper
<Key,
 Item: Identifiable & Hashable,
 LoadingContentOnConcatenate: View,
 ErrorContentOnConcatenate: View,
 ItemView: View>: View
{
    @ObservedObject var lazyPagingItems: LazyPagingItems<Key, Item>
    let sectionConfiguration: SectionConfiguration<Item>
    let loadingContentOnAppend: () -> LoadingContentOnConcatenate
    let errorContentOnAppend: (Error) -> ErrorContentOnConcatenate
    let itemContent: (Item) -> ItemView

    var body: some View {
        let indexedItems = Array(lazyPagingItems.enumerated())
        let sections = buildSections(from: indexedItems)

        ForEach(sections) { section in
            Section {
                ForEach(section.items, id: \.element.id) { index, item in
                    itemContent(item)
                        .task { await lazyPagingItems.appendIfNeeded(currentIndex: index) }
                }
            } header: {
                sectionConfiguration.header(section.key)
            }
        }

        if case .loading = lazyPagingItems.loadState.append {
            loadingContentOnAppend()
                .withDisableGestures()
        } else if case .failure(let error, _) = lazyPagingItems.loadState.append {
            errorContentOnAppend(error)
        }
    }

    private func buildSections(from items: [(offset: Int, element: Item)]) -> [ItemSection<Item>] {
        var sections: [ItemSection<Item>] = []
        var currentKey: AnyHashable?
        var currentItems: [(offset: Int, element: Item)] = []

        for item in items {
            let key = sectionConfiguration.key(item.element)
            if key != currentKey {
                if let currentKey, !currentItems.isEmpty {
                    sections.append(ItemSection(key: currentKey, items: currentItems))
                }
                currentKey = key
                currentItems = [item]
            } else {
                currentItems.append(item)
            }
        }
        if let currentKey, !currentItems.isEmpty {
            sections.append(ItemSection(key: currentKey, items: currentItems))
        }

        return sections
    }
}

private struct LoadingContentWrapper<LoadingContent: View>: View {
    let loadingContent: () -> LoadingContent

    var body: some View {
        loadingContent()
            .withDisableGestures()
    }
}

public extension StatefulLazyVStack
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
        pinnedViews: PinnedScrollableViews = [],
        spacing: CGFloat = 0,
        @ViewBuilder itemContent: @escaping (Item) -> ItemView
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
            pinnedViews: pinnedViews,
            spacing: spacing,
            itemContent: itemContent
        )
    }
}

public extension StatefulLazyVStack
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

private extension View {
    func withDisableGestures(when isDisabled: Bool = true) -> some View {
        self.allowsHitTesting(!isDisabled).scrollDisabled(isDisabled)
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
        refreshLoadingContent: { EmptyView() },
        emptyContent: { EmptyView() },
        errorContent: { _ in EmptyView() },
        loadingContentOnConcatenate: { EmptyView() },
        errorContentOnConcatenate: { _ in EmptyView() }
    ) { item in
        Text(item.id)
    }
}
