import SwiftUI
import UI
import Core
import LazyPaging

/// Another gambler's Timeline: their identity and pool total, then one kickoff group per item of
/// the grouped feed in the server's order, unsettled groups before finished ones. Identity and
/// total scroll with the groups and start every list state (initial loading, loaded, empty,
/// failed), so they stay usable whatever happens to the groups. Group headers scroll with the
/// rows; none is pinned.
struct BetTimelineList: View {
    var lazyPagingItems: LazyPaging.LazyPagingItems<String, KickoffGroupModel>
    let gamblerId: String
    let gamblerUsername: String
    let pointsSummary: HistoryPointsSummaryState
    let showsSummaryRefreshProgress: Bool
    let onPointsSummaryRetry: () -> Void
    let onRetryFirstPage: () -> Void
    let onRetryPage: (Error) -> Void
    let onMatchOpen: MatchOpenHandler?

    @Environment(\.boxSpacing) private var boxSpacing

    init(
        lazyPagingItems: LazyPaging.LazyPagingItems<String, KickoffGroupModel>,
        gamblerId: String,
        gamblerUsername: String,
        pointsSummary: HistoryPointsSummaryState,
        showsSummaryRefreshProgress: Bool = false,
        onPointsSummaryRetry: @escaping () -> Void,
        onRetryFirstPage: @escaping () -> Void,
        onRetryPage: @escaping (Error) -> Void,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self.lazyPagingItems = lazyPagingItems
        self.gamblerId = gamblerId
        self.gamblerUsername = gamblerUsername
        self.pointsSummary = pointsSummary
        self.showsSummaryRefreshProgress = showsSummaryRefreshProgress
        self.onPointsSummaryRetry = onPointsSummaryRetry
        self.onRetryFirstPage = onRetryFirstPage
        self.onRetryPage = onRetryPage
        self.onMatchOpen = onMatchOpen
    }

    private var owner: HistoryOwner {
        .selectedGambler(name: gamblerUsername)
    }

    var body: some View {
        let contentInsets = EdgeInsets(top: boxSpacing.medium, leading: 0, bottom: boxSpacing.medium, trailing: 0)
        // The plain paging stack: Timeline supplies its own pull to refresh, which reloads the
        // groups and the total together (see `BetTimelineListView`).
        AppendPagingVStack(
            lazyPagingItems: lazyPagingItems,
            // The spacing goes inside the scroll view, so rows scroll beneath the bars.
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
                    KickoffGroupSection(group: group, owner: owner, onMatchOpen: onMatchOpen)
                } else {
                    KickoffGroupPlaceholderList(count: 1)
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
                    LazyPagingCursorSource<KickoffGroupModel>(
                        pagingQuery: { _ in .success(CursorPage(items: timelineGroupPreviewModels(), next: nil)) }
                    )
                }
            )
        ),
        gamblerId: "",
        gamblerUsername: "El mono",
        pointsSummary: .initial.loaded(.earned(20)),
        onPointsSummaryRetry: {},
        onRetryFirstPage: {},
        onRetryPage: { _ in }
    )
}

#Preview("Total failed, rows loaded") {
    BetTimelineList(
        lazyPagingItems: LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 25, prefetchDistance: 5),
                pagingSourceFactory: {
                    LazyPagingCursorSource<KickoffGroupModel>(
                        pagingQuery: { _ in .success(CursorPage(items: timelineGroupPreviewModels(), next: nil)) }
                    )
                }
            )
        ),
        gamblerId: "",
        gamblerUsername: "El mono",
        pointsSummary: .initial.failed(),
        onPointsSummaryRetry: {},
        onRetryFirstPage: {},
        onRetryPage: { _ in }
    )
}

#Preview("Total loaded, rows failed") {
    BetTimelineList(
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
        gamblerId: "",
        gamblerUsername: "El mono",
        pointsSummary: .initial.loaded(.earned(4)),
        onPointsSummaryRetry: {},
        onRetryFirstPage: {},
        onRetryPage: { _ in }
    )
}

#Preview("Loading") {
    BetTimelineList(
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
        gamblerId: "",
        gamblerUsername: "El mono",
        pointsSummary: .initial.loading(),
        onPointsSummaryRetry: {},
        onRetryFirstPage: {},
        onRetryPage: { _ in }
    )
}
