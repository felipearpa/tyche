import SwiftUI
import UI
import Core
import DataBet

struct BetTimelineList: View {
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
            loadingContent: { BetsTimelinePlaceholderList(count: 50) },
            loadingContentOnConcatenate: {
                BetsTimelinePlaceholderItem()
                Divider()
            },
            sectionKey: { $0.matchDateTime.toShortDateString() },
            sectionHeader: { dateString in
                Text(dateString)
                    .font(.body)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, boxSpacing.medium)
                    .padding(.vertical, boxSpacing.medium)
            }
        ) { poolGamblerBet in
            VStack(spacing: 0) {
                BetTimelineItem(poolGamblerBet: poolGamblerBet)
                    .padding(.horizontal, boxSpacing.large)
                    .contentShape(Rectangle())
                    .onTapGesture { invokeMatchOpen(onMatchOpen, poolGamblerBet) }

                Divider()
                    .padding(.horizontal, boxSpacing.large)
                    .padding(.vertical, boxSpacing.medium)
            }
        }
    }
}

private struct BetsTimelinePlaceholderList: View {
    let count: Int

    var body: some View {
        ForEach(0..<count, id: \.self) { _ in
            BetsTimelinePlaceholderItem()
            Divider()
        }
    }
}

private struct BetsTimelinePlaceholderItem: View {
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        BetTimelineItem(poolGamblerBet: poolGamblerBetFakeModel())
            .shimmer()
            .padding(boxSpacing.large)
    }
}

#Preview {
    BetTimelineList(
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
