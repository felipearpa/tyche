import SwiftUI
import UI
import Core
import DataBet

struct LiveBetList: View {
    var lazyPagingItems: LazyPagingItems<String, PoolGamblerBetModel>

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        let _ = Self._printChangesIfDebug()

        RefreshableStatefulLazyVStack(
            lazyPagingItems: lazyPagingItems,
            loadingContent: { LiveBetPlaceholderList() },
            loadingContentOnConcatenate: {
                LiveBetPlaceholderItem()
                Divider()
            }
        ) { poolGamblerBet in
            NonEditablePendingBetItem(
                poolGamblerBet: poolGamblerBet,
                partialPoolGamblerBet: poolGamblerBet.toPartial()
            )
            .padding(boxSpacing.medium)
            Divider()
        }
    }
}

private struct LiveBetPlaceholderList: View {
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        ForEach(1...10, id: \.self) { _ in
            LiveBetPlaceholderItem()
                .padding(boxSpacing.medium)
            Divider()
        }
    }
}

private struct LiveBetPlaceholderItem: View {
    var body: some View {
        NonEditablePendingBetItem(
            poolGamblerBet: poolGamblerBetFakeModel(),
            partialPoolGamblerBet: partialPoolGamblerBetFakeModel()
        ).shimmer()
    }
}

#Preview("List") {
    LiveBetList(
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
