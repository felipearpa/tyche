import SwiftUI
import UI
import Core
import DataBet

struct BetsTimelineList: View {
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
            }
        ) { poolGamblerBet in
            VStack(spacing: 0) {
                if shouldShowHeader(for: poolGamblerBet) {
                    Text(poolGamblerBet.matchDateTime.toShortDateString())
                        .font(.title3)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, boxSpacing.medium)
                        .padding(.top, boxSpacing.medium)
                }

                BetTimelineItem(poolGamblerBet: poolGamblerBet)
                    .padding(boxSpacing.large)
                    .contentShape(Rectangle())
                    .onTapGesture { invokeMatchOpen(onMatchOpen, poolGamblerBet) }

                Divider()
            }
        }
    }

    private func shouldShowHeader(for bet: PoolGamblerBetModel) -> Bool {
        var previous: PoolGamblerBetModel?
        for item in lazyPagingItems {
            if item.id == bet.id {
                guard let previous else { return true }
                return previous.matchDateTime.toShortDateString() != bet.matchDateTime.toShortDateString()
            }
            previous = item
        }
        return true
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
    BetsTimelineList(
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
