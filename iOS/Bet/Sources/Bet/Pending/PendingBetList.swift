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
                    .frame(maxWidth: .infinity)
                    .padding(.horizontal, boxSpacing.large)
                    .padding(.vertical, boxSpacing.medium)
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
                Group {
                    if isInPreviewMode() {
                        PendingBetItem(
                            poolGamblerBet: poolGamblerBet,
                            viewState: .constant(PendingBetItemViewState.emptyVisualization())
                        )
                    } else {
                        PendingBetItemView(
                            viewModel: PendingBetItemViewModel(
                                betUseCase: diResolver.resolve(BetUseCase.self)!
                            ),
                            poolGamblerBet: poolGamblerBet
                        )
                    }
                }
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

func invokeMatchOpen(_ handler: MatchOpenHandler?, _ poolGamblerBet: PoolGamblerBetModel) {
    handler?(
        poolGamblerBet.poolId,
        poolGamblerBet.gamblerId,
        poolGamblerBet.matchId
    )
}

private struct PendingBetPlaceholderList: View {
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        ForEach(1...50, id: \.self) { _ in
            PendingBetPlaceholderItem()
                .frame(maxWidth: .infinity)
                .padding(.horizontal, boxSpacing.large)
                .padding(.vertical, boxSpacing.medium)
            Divider()
                .padding(.horizontal, boxSpacing.large)
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
