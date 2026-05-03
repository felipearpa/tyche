import SwiftUI
import UI
import Core
import DataBet

struct MatchBetList: View {
    var lazyPagingItems: LazyPagingItems<String, PoolGamblerBetModel>
    let onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)?

    @Environment(\.boxSpacing) private var boxSpacing

    init(
        lazyPagingItems: LazyPagingItems<String, PoolGamblerBetModel>,
        onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)? = nil
    ) {
        self.lazyPagingItems = lazyPagingItems
        self.onGamblerOpen = onGamblerOpen
    }

    var body: some View {
        let _ = Self._printChangesIfDebug()

        RefreshableStatefulLazyVStack(
            lazyPagingItems: lazyPagingItems,
            loadingContent: { MatchBetPlaceholderList() },
            loadingContentOnConcatenate: {
                VStack(spacing: 0) {
                    MatchBetPlaceholderItem()
                        .padding(boxSpacing.medium)
                    Divider()
                }
                .padding(.horizontal, boxSpacing.medium)
            }
        ) { poolGamblerBet in
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
        }
    }
}

private struct MatchBetPlaceholderList: View {
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        ForEach(1...10, id: \.self) { _ in
            VStack(spacing: 0) {
                MatchBetPlaceholderItem()
                    .padding(boxSpacing.medium)
                Divider()
            }
            .padding(.horizontal, boxSpacing.medium)
        }
    }
}

private struct MatchBetPlaceholderItem: View {
    var body: some View {
        MatchGamblerBetItem(poolGamblerBet: poolGamblerBetFakeModel()).shimmer()
    }
}

#Preview("List") {
    MatchBetList(
        lazyPagingItems: LazyPagingItems(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: {
                    PoolGamblerBetPagingSource(
                        pagingQuery: { _ in .success(CursorPage(items: poolGamblerBetDummyModels(), next: nil)) }
                    )
                }
            )
        )
    )
}
