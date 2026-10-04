import SwiftUI
import UI
import Core
import LazyPaging

/// Another gambler's Timeline: their identity and pool total, then one shared History row per
/// entry in the feed's order, uncomputed entries included. Identity and total scroll with the
/// rows and start every list state (initial loading, loaded, empty, failed), so they stay usable
/// whatever happens to the rows. Rows carry their own date, so there are no pinned date headers.
struct BetTimelineList: View {
    var lazyPagingItems: LazyPaging.LazyPagingItems<String, PoolGamblerBetModel>
    let gamblerId: String
    let gamblerUsername: String
    let pointsSummary: HistoryPointsSummaryState
    let showsSummaryRefreshProgress: Bool
    let onPointsSummaryRetry: () -> Void
    let onMatchOpen: MatchOpenHandler?

    @Environment(\.boxSpacing) private var boxSpacing

    init(
        lazyPagingItems: LazyPaging.LazyPagingItems<String, PoolGamblerBetModel>,
        gamblerId: String,
        gamblerUsername: String,
        pointsSummary: HistoryPointsSummaryState,
        showsSummaryRefreshProgress: Bool = false,
        onPointsSummaryRetry: @escaping () -> Void,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self.lazyPagingItems = lazyPagingItems
        self.gamblerId = gamblerId
        self.gamblerUsername = gamblerUsername
        self.pointsSummary = pointsSummary
        self.showsSummaryRefreshProgress = showsSummaryRefreshProgress
        self.onPointsSummaryRetry = onPointsSummaryRetry
        self.onMatchOpen = onMatchOpen
    }

    private var owner: HistoryOwner {
        .selectedGambler(name: gamblerUsername)
    }

    var body: some View {
        let contentInsets = EdgeInsets(top: boxSpacing.medium, leading: 0, bottom: boxSpacing.medium, trailing: 0)
        // The plain paging stack: Timeline supplies its own pull to refresh, which reloads the
        // rows and the total together (see `BetTimelineListView`).
        LazyPagingVStack(
            lazyPagingItems: lazyPagingItems,
            // The spacing goes inside the scroll view, so rows scroll beneath the bars.
            contentInsets: contentInsets,
            loadingContent: {
                header
                HistoryBetPlaceholderList(count: 50)
            },
            emptyContent: {
                LazyPagingVStackEmpty(header: { header })
            },
            errorContent: { error in
                // Retry reloads only the rows; the total keeps its own retry in the header.
                LazyPagingVStackError(
                    localizedError: error.orDefaultLocalized(),
                    retry: { Task { await lazyPagingItems.refresh() } },
                    header: { header }
                )
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
                        HistoryBetItem(poolGamblerBet: poolGamblerBet, owner: owner)
                            .historyHorizontalGutter()
                    }
                    .buttonStyle(InteractiveRowButtonStyle())

                    HistoryRowDivider()
                } else {
                    HistoryBetPlaceholderRow()
                }
            }
        }
        .fullListStateViewport(contentInsets: contentInsets)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: boxSpacing.medium) {
            TimelineGamblerHeading(gamblerId: gamblerId, gamblerUsername: gamblerUsername)

            HistoryPointsHeader(
                state: pointsSummary,
                owner: owner,
                showsRefreshProgress: showsSummaryRefreshProgress,
                onRetry: onPointsSummaryRetry
            )
        }
        .historyHorizontalGutter()
        .padding(.bottom, boxSpacing.large)
    }
}

#Preview("Loaded") {
    BetTimelineList(
        lazyPagingItems: LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 25, prefetchDistance: 5),
                pagingSourceFactory: {
                    LazyPagingCursorSource<PoolGamblerBetModel>(
                        pagingQuery: { _ in .success(CursorPage(items: timelineBetPreviewModels(), next: nil)) }
                    )
                }
            )
        ),
        gamblerId: "",
        gamblerUsername: "El mono",
        pointsSummary: .initial.loaded(.earned(20)),
        onPointsSummaryRetry: {}
    )
}

#Preview("Total failed, rows loaded") {
    BetTimelineList(
        lazyPagingItems: LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 25, prefetchDistance: 5),
                pagingSourceFactory: {
                    LazyPagingCursorSource<PoolGamblerBetModel>(
                        pagingQuery: { _ in .success(CursorPage(items: timelineBetPreviewModels(), next: nil)) }
                    )
                }
            )
        ),
        gamblerId: "",
        gamblerUsername: "El mono",
        pointsSummary: .initial.failed(),
        onPointsSummaryRetry: {}
    )
}

#Preview("Total loaded, rows failed") {
    BetTimelineList(
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
        gamblerId: "",
        gamblerUsername: "El mono",
        pointsSummary: .initial.loaded(.earned(4)),
        onPointsSummaryRetry: {}
    )
}

#Preview("Loading") {
    BetTimelineList(
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
        gamblerId: "",
        gamblerUsername: "El mono",
        pointsSummary: .initial.loading(),
        onPointsSummaryRetry: {}
    )
}
