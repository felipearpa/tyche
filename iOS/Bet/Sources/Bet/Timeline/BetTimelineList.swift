import SwiftUI
import UI
import Core
import LazyPaging
import DataBet

struct BetTimelineList: View {
    var lazyPagingItems: LazyPaging.LazyPagingItems<String, PoolGamblerBetModel>
    let onMatchOpen: MatchOpenHandler?

    @Environment(\.boxSpacing) private var boxSpacing

    init(
        lazyPagingItems: LazyPaging.LazyPagingItems<String, PoolGamblerBetModel>,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self.lazyPagingItems = lazyPagingItems
        self.onMatchOpen = onMatchOpen
    }

    var body: some View {
        RefreshableLazyPagingVStack(
            lazyPagingItems: lazyPagingItems,
            pinnedViews: [.sectionHeaders],
            loadingContent: { BetTimelinePlaceholderList(count: 50) },
            appendLoadingContent: { BetTimelinePlaceholderRow() },
        ) { index in
            if let poolGamblerBet = lazyPagingItems.peek(at: index) {
                let currentDate = poolGamblerBet.matchDateTime.toShortDateString()
                let previousDate = index > 0
                    ? lazyPagingItems.peek(at: index - 1)?.matchDateTime.toShortDateString()
                    : nil
                let isFirstInSection = currentDate != previousDate
                let isFirstSection = index == 0

                Section {
                    BetTimelineItem(poolGamblerBet: poolGamblerBet)
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, boxSpacing.large)
                        .padding(.vertical, boxSpacing.medium)
                        .contentShape(Rectangle())
                        .onTapGesture { invokeMatchOpen(onMatchOpen, poolGamblerBet) }

                    Divider()
                        .padding(.horizontal, boxSpacing.large)
                } header: {
                    if isFirstInSection {
                        Text(currentDate)
                            .font(.body)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, boxSpacing.medium)
                            .padding(.top, isFirstSection ? boxSpacing.medium : boxSpacing.medium + boxSpacing.medium)
                            .padding(.bottom, boxSpacing.medium)
                            .background(.background)
                    }
                }
            } else {
                Section {
                    BetTimelinePlaceholderItem()
                    Divider()
                        .padding(.horizontal, boxSpacing.large)
                }
            }
        }
    }
}

private struct BetTimelinePlaceholderList: View {
    let count: Int

    var body: some View {
        ForEach(0..<count, id: \.self) { _ in
            BetTimelinePlaceholderRow()
        }
    }
}

private struct BetTimelinePlaceholderRow: View {
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        BetTimelinePlaceholderItem()
        Divider()
            .padding(.horizontal, boxSpacing.large)
    }
}

private struct BetTimelinePlaceholderItem: View {
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        BetTimelineItem(poolGamblerBet: poolGamblerBetPlaceholderModel())
            .shimmer()
            .frame(maxWidth: .infinity)
            .padding(.horizontal, boxSpacing.large)
            .padding(.vertical, boxSpacing.medium)
    }
}

#Preview {
    BetTimelineList(
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
