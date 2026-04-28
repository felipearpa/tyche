import SwiftUI
import UI
import Core
import DataBet

struct PendingBetList: View {
    var lazyPagingItems: LazyPagingItems<String, PoolGamblerBetModel>
    let onMatchOpen: MatchOpenHandler?

    @Environment(\.boxSpacing) private var boxSpacing
    @Environment(\.diResolver) private var diResolver: DIResolver

    init(
        lazyPagingItems: LazyPagingItems<String, PoolGamblerBetModel>,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self.lazyPagingItems = lazyPagingItems
        self.onMatchOpen = onMatchOpen
    }

    var body: some View {
        let _ = Self._printChangesIfDebug()

        RefreshableStatefulLazyVStack(
            lazyPagingItems: lazyPagingItems,
            loadingContent: { PendingBetPlaceholderList() },
            loadingContentOnConcatenate: {
                PendingBetPlaceholderItem()
                Divider()
            }
        ) { poolGamblerBet in
            if isInPreviewMode() {
                PendingBetItem(
                    poolGamblerBet: poolGamblerBet,
                    viewState: .constant(PendingBetItemViewState.emptyVisualization())
                )
                .padding(boxSpacing.medium)
                .contentShape(Rectangle())
                .onTapGesture { invokeMatchOpen(onMatchOpen, poolGamblerBet) }
                Divider()
            } else {
                PendingBetItemView(
                    viewModel: PendingBetItemViewModel(
                        poolGamblerBet: poolGamblerBet,
                        betUseCase: diResolver.resolve(BetUseCase.self)!
                    )
                )
                .padding(boxSpacing.medium)
                .contentShape(Rectangle())
                .onTapGesture { invokeMatchOpen(onMatchOpen, poolGamblerBet) }
                Divider()
            }
        }
    }
}

func invokeMatchOpen(_ handler: MatchOpenHandler?, _ poolGamblerBet: PoolGamblerBetModel) {
    handler?(
        poolGamblerBet.poolId,
        poolGamblerBet.matchId,
        poolGamblerBet.homeTeamName,
        poolGamblerBet.awayTeamName,
        poolGamblerBet.matchDateTime,
        poolGamblerBet.matchScore?.homeTeamValue,
        poolGamblerBet.matchScore?.awayTeamValue
    )
}

private struct PendingBetPlaceholderList: View {
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        ForEach(1...50, id: \.self) { _ in
            PendingBetPlaceholderItem()
                .padding(boxSpacing.medium)
            Divider()
        }
    }
}

private struct PendingBetPlaceholderItem: View {
    var body: some View {
        PendingBetItem(
            poolGamblerBet: poolGamblerBetFakeModel(),
            viewState: .constant(.visualization(partialPoolGamblerBetFakeModel()))
        ).shimmer()
    }
}

#Preview("List") {
    PendingBetList(
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

#Preview("Placeholder") {
    ScrollView {
        LazyVStack(spacing: 0) {
            PendingBetPlaceholderList()
        }
    }
}
