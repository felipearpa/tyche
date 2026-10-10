import Core
import LazyPaging
import SwiftUI
import UI

/// The revealed match screen: the heading followed by one row per participant in the server's
/// pool-total order. The heading scrolls with the rows and starts every list state (initial
/// loading, loaded, empty, failed), so it stays usable whatever happens to the rows.
///
/// Another participant's row is one native button that opens their Timeline. The signed-in
/// gambler's row and loading rows have no action.
struct MatchBetList<Header: View>: View {
    let lazyPagingItems: LazyPaging.LazyPagingItems<String, PoolGamblerBetModel>
    let viewerAccountId: String
    let isComputed: Bool
    let header: Header
    let onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)?
    let onRetryFirstPage: () -> Void
    let onRetryPage: (Error) -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    init(
        lazyPagingItems: LazyPaging.LazyPagingItems<String, PoolGamblerBetModel>,
        viewerAccountId: String,
        isComputed: Bool,
        onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)? = nil,
        onRetryFirstPage: @escaping () -> Void,
        onRetryPage: @escaping (Error) -> Void,
        @ViewBuilder header: () -> Header
    ) {
        self.lazyPagingItems = lazyPagingItems
        self.viewerAccountId = viewerAccountId
        self.isComputed = isComputed
        self.onGamblerOpen = onGamblerOpen
        self.onRetryFirstPage = onRetryFirstPage
        self.onRetryPage = onRetryPage
        self.header = header()
    }

    var body: some View {
        let contentInsets = EdgeInsets(top: boxSpacing.medium, leading: 0, bottom: boxSpacing.medium, trailing: 0)
        // The plain paging stack: the screen supplies its own pull to refresh, which rechecks the
        // match before reloading the rows (see `MatchBetListView`).
        LazyPagingVStack(
            lazyPagingItems: lazyPagingItems,
            contentInsets: contentInsets,
            loadingContent: {
                paddedHeader
                MatchBetPlaceholderList(isComputed: isComputed)
            },
            emptyContent: {
                LazyPagingVStackEmpty(header: { paddedHeader })
            },
            errorContent: { error in
                if error.isMatchBetsReloadInProgress {
                    paddedHeader
                    MatchBetPlaceholderList(isComputed: isComputed)
                } else {
                    LazyPagingVStackError(
                        localizedError: error.matchBetsLocalized(),
                        retry: onRetryFirstPage,
                        header: { paddedHeader }
                    )
                }
            },
            prependLoadingContent: { EmptyView() },
            appendLoadingContent: { MatchBetPlaceholderRow(isComputed: isComputed) },
            prependErrorContent: { _ in EmptyView() },
            appendErrorContent: { error in
                if error.isMatchBetsReloadInProgress {
                    MatchBetPlaceholderRow(isComputed: isComputed)
                } else {
                    LazyPagingVStackConcatenateError(
                        localizedError: error.matchBetsLocalized(),
                        retry: { onRetryPage(error) }
                    )
                    .historyHorizontalGutter()
                    .padding(.vertical, boxSpacing.large)
                }
            }
        ) { index in
            VStack(spacing: 0) {
                if index == 0 {
                    paddedHeader
                }
                if let poolGamblerBet = lazyPagingItems.peek(at: index) {
                    participantRow(poolGamblerBet)
                    HistoryRowDivider()
                } else {
                    MatchBetPlaceholderRow(isComputed: isComputed)
                }
            }
        }
        .fullListStateViewport(contentInsets: contentInsets)
    }

    private var paddedHeader: some View {
        header
            .historyHorizontalGutter()
            .padding(.bottom, boxSpacing.large)
    }

    @ViewBuilder
    private func participantRow(_ poolGamblerBet: PoolGamblerBetModel) -> some View {
        let isCurrentUser = poolGamblerBet.gamblerId == viewerAccountId
        if let onGamblerOpen, !isCurrentUser {
            Button {
                onGamblerOpen(poolGamblerBet.poolId, poolGamblerBet.gamblerId, poolGamblerBet.gamblerUsername)
            } label: {
                MatchGamblerBetItem(poolGamblerBet: poolGamblerBet)
            }
            .buttonStyle(InteractiveRowButtonStyle())
        } else {
            MatchGamblerBetItem(poolGamblerBet: poolGamblerBet, isCurrentUser: isCurrentUser)
        }
    }
}

/// The initial-load participant rows: production rows from placeholder models.
struct MatchBetPlaceholderList: View {
    let isComputed: Bool

    var body: some View {
        ForEach(0..<placeholderRowCount, id: \.self) { _ in
            MatchBetPlaceholderRow(isComputed: isComputed)
        }
    }
}

/// One loading row: the production row from a placeholder model, with its separator. It is
/// never wrapped in the Timeline button that loaded rows use.
struct MatchBetPlaceholderRow: View {
    let isComputed: Bool

    var body: some View {
        VStack(spacing: 0) {
            MatchGamblerBetItem(
                poolGamblerBet: matchGamblerBetPlaceholderModel(isComputed: isComputed),
                isPlaceholder: true
            )
            HistoryRowDivider()
        }
    }
}

private let placeholderRowCount = 12

#Preview("Computed") {
    let match = matchHeaderPreviewCases()[3].1
    MatchBetList(
        lazyPagingItems: LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 25, prefetchDistance: 5),
                pagingSourceFactory: {
                    LazyPagingCursorSource<PoolGamblerBetModel>(
                        pagingQuery: { _ in
                            .success(CursorPage(items: matchParticipantPreviewModels(isComputed: true), next: nil))
                        }
                    )
                }
            )
        ),
        viewerAccountId: matchPreviewViewerId,
        isComputed: true,
        onGamblerOpen: { _, _, _ in },
        onRetryFirstPage: {},
        onRetryPage: { _ in }
    ) {
        MatchHeader(match: match, presentation: .computed)
    }
}

#Preview("Bets closed, ranking unavailable") {
    let match = matchHeaderPreviewCases()[1].1
    MatchBetList(
        lazyPagingItems: LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 25, prefetchDistance: 5),
                pagingSourceFactory: {
                    LazyPagingCursorSource<PoolGamblerBetModel>(
                        pagingQuery: { _ in .failure(MatchBetsLocalizedError.rankingUnavailable) }
                    )
                }
            )
        ),
        viewerAccountId: matchPreviewViewerId,
        isComputed: false,
        onRetryFirstPage: {},
        onRetryPage: { _ in }
    ) {
        MatchHeader(match: match, presentation: .betsClosed)
    }
}
