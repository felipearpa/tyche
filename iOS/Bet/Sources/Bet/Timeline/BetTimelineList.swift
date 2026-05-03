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
            loadingContent: { BetTimelinePlaceholderList(count: 50) },
            loadingContentOnConcatenate: {
                BetTimelinePlaceholderItem()
                Divider()
                    .padding(.horizontal, boxSpacing.large)
            },
            sectionKey: { $0.matchDateTime.toShortDateString() },
            sectionHeader: { dateString, isFirst in
                Text(dateString)
                    .font(.body)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, boxSpacing.medium)
                    .padding(.top, isFirst ? boxSpacing.medium : boxSpacing.medium + boxSpacing.medium)
                    .padding(.bottom, boxSpacing.medium)
            }
        ) { poolGamblerBet in
            VStack(spacing: 0) {
                BetTimelineItem(poolGamblerBet: poolGamblerBet)
                    .frame(maxWidth: .infinity)
                    .padding(.horizontal, boxSpacing.large)
                    .padding(.vertical, boxSpacing.medium)
                    .contentShape(Rectangle())
                    .onTapGesture { invokeMatchOpen(onMatchOpen, poolGamblerBet) }

                Divider()
                    .padding(.horizontal, boxSpacing.large)
            }
        }
    }
}

private struct BetTimelinePlaceholderList: View {
    let count: Int

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        ForEach(0..<count, id: \.self) { _ in
            BetTimelinePlaceholderItem()
            Divider()
                .padding(.horizontal, boxSpacing.large)
        }
    }
}

private struct BetTimelinePlaceholderItem: View {
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        BetTimelineItem(poolGamblerBet: poolGamblerBetFakeModel())
            .shimmer()
            .frame(maxWidth: .infinity)
            .padding(.horizontal, boxSpacing.large)
            .padding(.vertical, boxSpacing.medium)
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
