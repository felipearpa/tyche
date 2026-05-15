import SwiftUI
import UI
import Core
import LazyPaging
import DataBet

struct MatchBetList: View {
    var lazyPagingItems: LazyPaging.LazyPagingItems<String, PoolGamblerBetModel>
    let onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)?

    @Environment(\.boxSpacing) private var boxSpacing

    init(
        lazyPagingItems: LazyPaging.LazyPagingItems<String, PoolGamblerBetModel>,
        onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)? = nil
    ) {
        self.lazyPagingItems = lazyPagingItems
        self.onGamblerOpen = onGamblerOpen
    }

    var body: some View {
        let _ = Self._printChangesIfDebug()

        RefreshableLazyPagingVStack(
            lazyPagingItems: lazyPagingItems,
            loadingContent: { MatchBetPlaceholderList() },
            appendLoadingContent: { MatchBetPlaceholderRow() },
        ) { index in
            if let poolGamblerBet = lazyPagingItems.peek(at: index) {
                if let onGamblerOpen {
                    VStack(spacing: 0) {
                        MatchGamblerBetItem(poolGamblerBet: poolGamblerBet)
                            .padding(boxSpacing.medium)
                        Divider()
                    }
                    .padding(.horizontal, boxSpacing.medium)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        onGamblerOpen(
                            poolGamblerBet.poolId,
                            poolGamblerBet.gamblerId,
                            poolGamblerBet.gamblerUsername
                        )
                    }
                } else {
                    VStack(spacing: 0) {
                        MatchGamblerBetItem(poolGamblerBet: poolGamblerBet)
                            .padding(boxSpacing.medium)
                        Divider()
                    }
                    .padding(.horizontal, boxSpacing.medium)
                }
            } else {
                VStack(spacing: 0) {
                    MatchBetPlaceholderItem()
                        .padding(boxSpacing.medium)
                    Divider()
                }
                .padding(.horizontal, boxSpacing.medium)
            }
        }
    }
}

private struct MatchBetPlaceholderList: View {
    var body: some View {
        ForEach(1...10, id: \.self) { _ in
            MatchBetPlaceholderRow()
        }
    }
}

private struct MatchBetPlaceholderRow: View {
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: 0) {
            MatchBetPlaceholderItem()
                .padding(boxSpacing.medium)
            Divider()
        }
        .padding(.horizontal, boxSpacing.medium)
    }
}

private struct MatchBetPlaceholderItem: View {
    var body: some View {
        MatchGamblerBetItem(poolGamblerBet: poolGamblerBetPlaceholderModel()).shimmer()
    }
}

#Preview("List") {
    MatchBetList(
        lazyPagingItems: LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 25, prefetchDistance: 5),
                pagingSourceFactory: {
                    LazyPagingCursorSource<PoolGamblerBetModel>(
                        pagingQuery: { _ in .success(CursorPage(items: poolGamblerBetDummyModels(), next: nil)) }
                    )
                }
            )
        )
    )
}
