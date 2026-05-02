import SwiftUI
import UI
import Core

struct FinishedBetList: View {
    var lazyPagingItems: LazyPagingItems<String, PoolGamblerBetModel>
    let onMatchOpen: MatchOpenHandler?

    @Environment(\.boxSpacing) private var boxSpacing

    init(
        lazyPagingItems: LazyPagingItems<String, PoolGamblerBetModel>,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self.lazyPagingItems = lazyPagingItems
        self.onMatchOpen = onMatchOpen
    }

    var body: some View {
        StatefulLazyVStack(
            lazyPagingItems: lazyPagingItems,
            loadingContent: { FinishedPoolGamblerBetFakeList(count: 50) },
            loadingContentOnConcatenate: {
                FinishedPoolGamblerBetFakeItem()
            },
            sectionKey: { $0.matchDateTime.toShortDateString() },
            sectionHeader: { dateString in
                Text(dateString)
                    .font(.title3)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, boxSpacing.large)
                    .padding(.top, boxSpacing.medium)
            }
        ) { poolGamblerBet in
            VStack(spacing: 0) {
                FinishedBetItem(poolGamblerBet: poolGamblerBet)
                    .padding(boxSpacing.large)
                Divider()
            }
            .padding(.horizontal, boxSpacing.medium)
            .contentShape(Rectangle())
            .onTapGesture { invokeMatchOpen(onMatchOpen, poolGamblerBet) }
        }
    }
}

private struct FinishedPoolGamblerBetFakeList: View {
    let count: Int

    var body: some View {
        ForEach(0..<count, id: \.self) { _ in
            FinishedPoolGamblerBetFakeItem()
        }
    }
}

private struct FinishedPoolGamblerBetFakeItem: View {
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: 0) {
            FinishedBetItem(poolGamblerBet: poolGamblerBetFakeModel())
                .shimmer()
                .padding(boxSpacing.large)
            Divider()
        }
        .padding(.horizontal, boxSpacing.medium)
    }
}

#Preview {
    FinishedBetList(
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
