import SwiftUI
import UI
import Core
import LazyPaging

struct FinishedBetList: View {
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
            loadingContent: { FinishedPoolGamblerBetFakeList(count: 50) },
            appendLoadingContent: { FinishedPoolGamblerBetFakeItem() },
        ) { index in
            if let poolGamblerBet = lazyPagingItems.peek(at: index) {
                let currentDate = poolGamblerBet.matchDateTime.toShortDateString()
                let previousDate = index > 0
                    ? lazyPagingItems.peek(at: index - 1)?.matchDateTime.toShortDateString()
                    : nil
                let isFirstInSection = currentDate != previousDate
                let isFirstSection = index == 0

                Section {
                    FinishedBetItem(poolGamblerBet: poolGamblerBet)
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
                            .font(.title3)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, boxSpacing.large)
                            .padding(.top, isFirstSection ? boxSpacing.medium : boxSpacing.medium + boxSpacing.medium)
                            .background(.background)
                    }
                }
            } else {
                Section {
                    FinishedPoolGamblerBetFakeItem()
                }
            }
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
        FinishedBetItem(poolGamblerBet: poolGamblerBetPlaceholderModel())
            .shimmer()
            .frame(maxWidth: .infinity)
            .padding(.horizontal, boxSpacing.large)
            .padding(.vertical, boxSpacing.medium)
        Divider()
            .padding(.horizontal, boxSpacing.large)
    }
}

#Preview {
    FinishedBetList(
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
