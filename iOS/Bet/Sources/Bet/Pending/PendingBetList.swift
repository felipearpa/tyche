import SwiftUI
import UI
import Core
import DataBet

struct PendingBetList: View {
    var lazyPagingItems: LazyPagingItems<String, PoolGamblerBetModel>

    @Environment(\.boxSpacing) private var boxSpacing
    @Environment(\.diResolver) private var diResolver: DIResolver

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
                Divider()
            } else {
                PendingBetItemView(
                    viewModel: PendingBetItemViewModel(
                        poolGamblerBet: poolGamblerBet,
                        betUseCase: diResolver.resolve(BetUseCase.self)!
                    )
                )
                .padding(boxSpacing.medium)
                Divider()
            }
        }
    }
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
