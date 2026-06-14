import SwiftUI
import UI
import Core
import LazyPaging
import DataBet

struct PendingBetList: View {
    var lazyPagingItems: LazyPaging.LazyPagingItems<String, PoolGamblerBetModel>
    let onMatchOpen: MatchOpenHandler?

    @Environment(\.boxSpacing) private var boxSpacing
    @Environment(\.diResolver) private var diResolver: DIResolver

    init(
        lazyPagingItems: LazyPaging.LazyPagingItems<String, PoolGamblerBetModel>,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self.lazyPagingItems = lazyPagingItems
        self.onMatchOpen = onMatchOpen
    }

    var body: some View {
        let _ = Self._printChangesIfDebug()

        RefreshableLazyPagingVStack(
            lazyPagingItems: lazyPagingItems,
            pinnedViews: [.sectionHeaders],
            loadingContent: { PendingBetPlaceholderList() },
            appendLoadingContent: { PendingBetPlaceholderRow() },
        ) { index in
            if let poolGamblerBet = lazyPagingItems.peek(at: index) {
                let currentDate = poolGamblerBet.matchDateTime.toShortDateString()
                let previousDate = index > 0
                    ? lazyPagingItems.peek(at: index - 1)?.matchDateTime.toShortDateString()
                    : nil
                let isFirstInSection = currentDate != previousDate
                let isFirstSection = index == 0

                Section {
                    Group {
                        if isInPreviewMode() {
                            StatefulPendingBetItemView(
                                viewModelState: .idle(poolGamblerBet),
                                viewState: .constant(PendingBetItemViewState.visualization(poolGamblerBet.toPartialPoolGamblerBet()))
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
                    .onMatchOpenTap(for: poolGamblerBet, onMatchOpen)

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
                    PendingBetPlaceholderItem()
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, boxSpacing.large)
                        .padding(.vertical, boxSpacing.medium)
                    Divider()
                        .padding(.horizontal, boxSpacing.large)
                }
            }
        }
    }
}

func invokeMatchOpen(_ handler: MatchOpenHandler?, _ poolGamblerBet: PoolGamblerBetModel) {
    if (!poolGamblerBet.isPending) {
        handler?(
            poolGamblerBet.poolId,
            poolGamblerBet.gamblerId,
            poolGamblerBet.matchId
        )
    }
}

private extension View {
    /// Opens the match on tap, but only for non-pending rows. Pending rows hold
    /// the editable bet text fields, so a row-wide tap recogniser there would
    /// compete with the fields for the tap and break focus.
    @ViewBuilder
    func onMatchOpenTap(for poolGamblerBet: PoolGamblerBetModel, _ handler: MatchOpenHandler?) -> some View {
        if poolGamblerBet.isPending {
            self
        } else {
            self
                .contentShape(Rectangle())
                .onTapGesture { invokeMatchOpen(handler, poolGamblerBet) }
        }
    }
}

private struct PendingBetPlaceholderList: View {
    var body: some View {
        ForEach(1...50, id: \.self) { _ in
            PendingBetPlaceholderRow()
        }
    }
}

private struct PendingBetPlaceholderRow: View {
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        PendingBetPlaceholderItem()
            .frame(maxWidth: .infinity)
            .padding(.horizontal, boxSpacing.large)
            .padding(.vertical, boxSpacing.medium)
        Divider()
            .padding(.horizontal, boxSpacing.large)
    }
}

private struct PendingBetPlaceholderItem: View {
    var body: some View {
        PendingBetItem(
            poolGamblerBet: poolGamblerBetPlaceholderModel(),
            viewState: .constant(.visualization(partialPoolGamblerBetFakeModel()))
        ).shimmer()
    }
}

#Preview("List") {
    PendingBetList(
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

#Preview("Placeholder") {
    ScrollView {
        LazyVStack(spacing: 0) {
            PendingBetPlaceholderList()
        }
    }
}
