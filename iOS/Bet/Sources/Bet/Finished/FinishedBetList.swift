import SwiftUI
import UI
import Core
import LazyPaging

/// The signed-in gambler's History: the earned-points summary followed by one row per finished
/// match, newest first. Every list state (initial loading, loaded, empty, failed) starts with the
/// same summary header, so a known total survives list changes. Rows carry their own date, so
/// there are no pinned date headers.
struct FinishedBetList: View {
    var lazyPagingItems: LazyPaging.LazyPagingItems<String, PoolGamblerBetModel>
    let pointsSummary: HistoryPointsSummaryState
    let showsSummaryRefreshProgress: Bool
    let onPointsSummaryRetry: () -> Void
    let onMatchOpen: MatchOpenHandler?

    @Environment(\.boxSpacing) private var boxSpacing

    init(
        lazyPagingItems: LazyPaging.LazyPagingItems<String, PoolGamblerBetModel>,
        pointsSummary: HistoryPointsSummaryState,
        showsSummaryRefreshProgress: Bool = false,
        onPointsSummaryRetry: @escaping () -> Void,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self.lazyPagingItems = lazyPagingItems
        self.pointsSummary = pointsSummary
        self.showsSummaryRefreshProgress = showsSummaryRefreshProgress
        self.onPointsSummaryRetry = onPointsSummaryRetry
        self.onMatchOpen = onMatchOpen
    }

    var body: some View {
        // The plain paging stack: History supplies its own pull to refresh, which reloads the rows
        // and the total together (see `FinishedBetListView`).
        LazyPagingVStack(
            lazyPagingItems: lazyPagingItems,
            // The spacing goes inside the scroll view: an outer padding would keep it off the
            // bars' safe-area edges, so rows would stop short of them instead of scrolling beneath.
            contentInsets: EdgeInsets(top: boxSpacing.medium, leading: 0, bottom: boxSpacing.medium, trailing: 0),
            loadingContent: {
                header
                HistoryBetPlaceholderList(count: 50)
            },
            emptyContent: {
                header
                LazyPagingVStackEmpty()
            },
            errorContent: { error in
                header
                LazyPagingVStackError(localizedError: error.orDefaultLocalized())
            },
            prependLoadingContent: { EmptyView() },
            appendLoadingContent: { HistoryBetPlaceholderRow() },
            prependErrorContent: { _ in EmptyView() },
            appendErrorContent: { error in
                LazyPagingVStackConcatenateError(
                    localizedError: error.orDefaultLocalized(),
                    retry: { Task { await lazyPagingItems.retry() } }
                )
                .historyHorizontalGutter()
                .padding(.vertical, boxSpacing.large)
            }
        ) { index in
            VStack(spacing: 0) {
                if index == 0 {
                    header
                }
                if let poolGamblerBet = lazyPagingItems.peek(at: index) {
                    Button {
                        invokeMatchOpen(onMatchOpen, poolGamblerBet)
                    } label: {
                        HistoryBetItem(poolGamblerBet: poolGamblerBet)
                            .historyHorizontalGutter()
                    }
                    .buttonStyle(InteractiveRowButtonStyle())

                    HistoryRowDivider()
                } else {
                    HistoryBetPlaceholderRow()
                }
            }
        }
    }

    private var header: some View {
        HistoryPointsHeader(
            state: pointsSummary,
            showsRefreshProgress: showsSummaryRefreshProgress,
            onRetry: onPointsSummaryRetry
        )
        .historyHorizontalGutter()
        .padding(.bottom, boxSpacing.large)
    }
}

private struct HistoryRowDivider: View {
    var body: some View {
        Divider()
            .historyHorizontalGutter()
    }
}

private struct HistoryBetPlaceholderList: View {
    let count: Int

    var body: some View {
        ForEach(0..<count, id: \.self) { _ in
            HistoryBetPlaceholderRow()
        }
    }
}

private struct HistoryBetPlaceholderRow: View {
    var body: some View {
        VStack(spacing: 0) {
            HistoryBetItem(poolGamblerBet: historyBetPlaceholderModel(), isPlaceholder: true)
                .historyHorizontalGutter()
            HistoryRowDivider()
        }
    }
}

#Preview("Loaded") {
    FinishedBetList(
        lazyPagingItems: LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 25, prefetchDistance: 5),
                pagingSourceFactory: {
                    LazyPagingCursorSource<PoolGamblerBetModel>(
                        pagingQuery: { _ in .success(CursorPage(items: historyBetPreviewModels(), next: nil)) }
                    )
                }
            )
        ),
        pointsSummary: .initial.loaded(.earned(120)),
        onPointsSummaryRetry: {}
    )
}

#Preview("Summary failed, rows loaded") {
    FinishedBetList(
        lazyPagingItems: LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 25, prefetchDistance: 5),
                pagingSourceFactory: {
                    LazyPagingCursorSource<PoolGamblerBetModel>(
                        pagingQuery: { _ in .success(CursorPage(items: historyBetPreviewModels(), next: nil)) }
                    )
                }
            )
        ),
        pointsSummary: .initial.failed(),
        onPointsSummaryRetry: {}
    )
}

#Preview("Total loaded, rows failed") {
    FinishedBetList(
        lazyPagingItems: LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 25, prefetchDistance: 5),
                pagingSourceFactory: {
                    LazyPagingCursorSource<PoolGamblerBetModel>(
                        pagingQuery: { _ in .failure(UnknownLocalizedError()) }
                    )
                }
            )
        ),
        pointsSummary: .initial.loaded(.earned(4)),
        onPointsSummaryRetry: {}
    )
}

#Preview("Loading") {
    FinishedBetList(
        lazyPagingItems: LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 25, prefetchDistance: 5),
                pagingSourceFactory: {
                    LazyPagingCursorSource<PoolGamblerBetModel>(
                        pagingQuery: { _ in
                            try? await Task.sleep(for: .seconds(3600))
                            return .success(CursorPage(items: [], next: nil))
                        }
                    )
                }
            )
        ),
        pointsSummary: .initial.loading(),
        onPointsSummaryRetry: {}
    )
}
