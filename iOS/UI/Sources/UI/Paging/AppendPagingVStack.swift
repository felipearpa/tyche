import SwiftUI
import LazyPaging

/// A paging list for feeds that only grow downward: the content of `LazyPaging.LazyPagingVStack`,
/// except that an appearing row only asks for the next page and never for a previous one.
///
/// `LazyPagingItems.retry()` repeats the most recently armed edge load. `LazyPagingVStack` arms
/// both edges for every row it shows, so once a row near the top appears after an append failed
/// (a short list, or scrolling back up), its prepend becomes the retried load and the append
/// error's Retry does nothing for a feed without previous pages. Arming only the append here keeps
/// that Retry on the failed append. Cursor feeds (`LazyPagingCursorSource`) never prepend.
public struct AppendPagingVStack<
    Key: Hashable & Sendable,
    Item: Identifiable & Hashable & Sendable,
    LoadingContent: View,
    EmptyContent: View,
    ErrorContent: View,
    AppendLoadingContent: View,
    AppendErrorContent: View,
    RowContent: View
>: View {
    @ObservedObject private var lazyPagingItems: LazyPaging.LazyPagingItems<Key, Item>

    private let contentInsets: EdgeInsets
    private let loadingContent: () -> LoadingContent
    private let emptyContent: () -> EmptyContent
    private let errorContent: (any Error) -> ErrorContent
    private let appendLoadingContent: () -> AppendLoadingContent
    private let appendErrorContent: (any Error) -> AppendErrorContent
    private let rowContent: (Int) -> RowContent

    public init(
        lazyPagingItems: LazyPaging.LazyPagingItems<Key, Item>,
        contentInsets: EdgeInsets = EdgeInsets(),
        @ViewBuilder loadingContent: @escaping () -> LoadingContent,
        @ViewBuilder emptyContent: @escaping () -> EmptyContent,
        @ViewBuilder errorContent: @escaping (any Error) -> ErrorContent,
        @ViewBuilder appendLoadingContent: @escaping () -> AppendLoadingContent,
        @ViewBuilder appendErrorContent: @escaping (any Error) -> AppendErrorContent,
        @ViewBuilder rowContent: @escaping (Int) -> RowContent
    ) {
        self._lazyPagingItems = ObservedObject(wrappedValue: lazyPagingItems)
        self.contentInsets = contentInsets
        self.loadingContent = loadingContent
        self.emptyContent = emptyContent
        self.errorContent = errorContent
        self.appendLoadingContent = appendLoadingContent
        self.appendErrorContent = appendErrorContent
        self.rowContent = rowContent
    }

    public var body: some View {
        // Before the first refresh completes there are no items; hinting `.loading` keeps the
        // loading content up instead of an empty area, as `LazyPagingVStack` does.
        let state = resolveLazyPagingVStackState(
            refresh: lazyPagingItems.loadState.refresh,
            prepend: lazyPagingItems.loadState.prepend,
            append: lazyPagingItems.loadState.append,
            itemCount: lazyPagingItems.itemCount,
            current: lazyPagingItems.itemCount > 0 ? .content : .loading
        )

        ScrollView {
            LazyVStack(spacing: 0) {
                switch state {
                case .loading:
                    loadingContent()
                case .empty:
                    emptyContent()
                case .error(let error):
                    errorContent(error)
                case .content:
                    ForEach(rows) { row in
                        rowContent(row.index)
                            .task { await Self.rowDidAppear(lazyPagingItems, at: row.index) }
                    }
                    switch lazyPagingItems.loadState.append {
                    case .loading:
                        appendLoadingContent()
                    case .failure(let error):
                        appendErrorContent(error)
                    case .notLoading:
                        EmptyView()
                    }
                }
            }
            .padding(contentInsets)
        }
        // Keyed on the items instance, so a new `LazyPagingItems` loads its first page. SwiftUI
        // can start and cancel this task within one update; a cancelled one must not refresh.
        .task(id: ObjectIdentifier(lazyPagingItems)) {
            guard !Task.isCancelled else { return }
            await lazyPagingItems.refresh()
        }
    }

    /// Loaded rows keep their item's identity across pages; an unloaded slot keys on its position.
    private var rows: [AppendPagingRow] {
        (0..<lazyPagingItems.itemCount).map { index in
            AppendPagingRow(
                index: index,
                id: lazyPagingItems.peek(at: index).map { AnyHashable($0.id) }
                    ?? AnyHashable("_append_paging_slot_\(index)")
            )
        }
    }

    /// What a row's appearance asks of the pager: the next page when the row is near the end.
    @MainActor
    static func rowDidAppear(_ lazyPagingItems: LazyPaging.LazyPagingItems<Key, Item>, at index: Int) async {
        await lazyPagingItems.appendIfNeeded(currentIndex: index)
    }
}

private struct AppendPagingRow: Identifiable {
    let index: Int
    let id: AnyHashable
}
