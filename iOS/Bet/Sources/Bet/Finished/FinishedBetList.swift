import SwiftUI
import UI
import Core
import LazyPaging

/// The signed-in gambler's History: the earned-points summary followed by one kickoff group per
/// item of the grouped feed, newest kickoff first. Every list state (initial loading, loaded,
/// empty, failed) starts with the same summary header, so a known total survives list changes.
/// Group headers scroll with the rows; none is pinned.
struct FinishedBetList: View {
    var lazyPagingItems: LazyPaging.LazyPagingItems<String, KickoffGroupModel>
    let pointsSummary: HistoryPointsSummaryState
    let showsSummaryRefreshProgress: Bool
    let onPointsSummaryRetry: () -> Void
    let onRetryFirstPage: () -> Void
    let onRetryPage: (Error) -> Void
    let onMatchOpen: MatchOpenHandler?

    @Environment(\.boxSpacing) private var boxSpacing

    init(
        lazyPagingItems: LazyPaging.LazyPagingItems<String, KickoffGroupModel>,
        pointsSummary: HistoryPointsSummaryState,
        showsSummaryRefreshProgress: Bool = false,
        onPointsSummaryRetry: @escaping () -> Void,
        onRetryFirstPage: @escaping () -> Void,
        onRetryPage: @escaping (Error) -> Void,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self.lazyPagingItems = lazyPagingItems
        self.pointsSummary = pointsSummary
        self.showsSummaryRefreshProgress = showsSummaryRefreshProgress
        self.onPointsSummaryRetry = onPointsSummaryRetry
        self.onRetryFirstPage = onRetryFirstPage
        self.onRetryPage = onRetryPage
        self.onMatchOpen = onMatchOpen
    }

    var body: some View {
        let contentInsets = EdgeInsets(top: boxSpacing.medium, leading: 0, bottom: boxSpacing.medium, trailing: 0)
        // The plain paging stack: History supplies its own pull to refresh, which reloads the
        // groups and the total together (see `FinishedBetListView`).
        AppendPagingVStack(
            lazyPagingItems: lazyPagingItems,
            // The spacing goes inside the scroll view: an outer padding would keep it off the
            // bars' safe-area edges, so rows would stop short of them instead of scrolling beneath.
            contentInsets: contentInsets,
            loadingContent: {
                header
                KickoffGroupPlaceholderList(count: initialPlaceholderGroupCount)
            },
            emptyContent: {
                LazyPagingVStackEmpty(header: { header })
            },
            errorContent: { error in
                if error.isKickoffGroupReloadInProgress {
                    header
                    KickoffGroupPlaceholderList(count: initialPlaceholderGroupCount)
                } else {
                    // Retry reloads only the groups; the total keeps its own retry in the header.
                    LazyPagingVStackError(
                        localizedError: error.kickoffGroupFeedLocalized(),
                        retry: onRetryFirstPage,
                        header: { header }
                    )
                }
            },
            appendLoadingContent: { KickoffGroupPlaceholderList(count: 1) },
            appendErrorContent: { error in
                if error.isKickoffGroupReloadInProgress {
                    KickoffGroupPlaceholderList(count: 1)
                } else {
                    LazyPagingVStackConcatenateError(
                        localizedError: error.kickoffGroupFeedLocalized(),
                        retry: { onRetryPage(error) }
                    )
                    .historyHorizontalGutter()
                    .padding(.vertical, boxSpacing.large)
                }
            }
        ) { index in
            VStack(spacing: 0) {
                if index == 0 {
                    header
                }
                if let group = lazyPagingItems.peek(at: index) {
                    KickoffGroupSection(group: group, onMatchOpen: onMatchOpen)
                } else {
                    KickoffGroupPlaceholderList(count: 1)
                }
            }
        }
        .fullListStateViewport(contentInsets: contentInsets)
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

/// Enough loading groups to fill a large phone; groups hold one to four rows.
let initialPlaceholderGroupCount = 6

/// The thin separator below each row of the match screen's participant list.
struct HistoryRowDivider: View {
    var body: some View {
        Divider()
            .historyHorizontalGutter()
    }
}

#Preview("Loaded") {
    FinishedBetList(
        lazyPagingItems: LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 25, prefetchDistance: 5),
                pagingSourceFactory: {
                    LazyPagingCursorSource<KickoffGroupModel>(
                        pagingQuery: { _ in .success(CursorPage(items: historyGroupPreviewModels(), next: nil)) }
                    )
                }
            )
        ),
        pointsSummary: .initial.loaded(.earned(120)),
        onPointsSummaryRetry: {},
        onRetryFirstPage: {},
        onRetryPage: { _ in }
    )
}

#Preview("Summary failed, rows loaded") {
    FinishedBetList(
        lazyPagingItems: LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 25, prefetchDistance: 5),
                pagingSourceFactory: {
                    LazyPagingCursorSource<KickoffGroupModel>(
                        pagingQuery: { _ in .success(CursorPage(items: historyGroupPreviewModels(), next: nil)) }
                    )
                }
            )
        ),
        pointsSummary: .initial.failed(),
        onPointsSummaryRetry: {},
        onRetryFirstPage: {},
        onRetryPage: { _ in }
    )
}

#Preview("Total loaded, rows failed") {
    FinishedBetList(
        lazyPagingItems: LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 25, prefetchDistance: 5),
                pagingSourceFactory: {
                    LazyPagingCursorSource<KickoffGroupModel>(
                        pagingQuery: { _ in .failure(UnknownLocalizedError()) }
                    )
                }
            )
        ),
        pointsSummary: .initial.loaded(.earned(4)),
        onPointsSummaryRetry: {},
        onRetryFirstPage: {},
        onRetryPage: { _ in }
    )
}

#Preview("Loading") {
    FinishedBetList(
        lazyPagingItems: LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 25, prefetchDistance: 5),
                pagingSourceFactory: {
                    LazyPagingCursorSource<KickoffGroupModel>(
                        pagingQuery: { _ in
                            try? await Task.sleep(for: .seconds(3600))
                            return .success(CursorPage(items: [], next: nil))
                        }
                    )
                }
            )
        ),
        pointsSummary: .initial.loading(),
        onPointsSummaryRetry: {},
        onRetryFirstPage: {},
        onRetryPage: { _ in }
    )
}
