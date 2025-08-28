import SwiftUI
import UI
import Core
import DataBet

struct PoolGamblerBetList: View {
    var lazyPagingItems: LazyPagingItems<String, PoolGamblerBetModel>

    @Environment(\.boxSpacing) private var boxSpacing
    @Environment(\.diResolver) private var diResolver: DIResolver

    var body: some View {
        let _ = Self._printChangesIfDebug()

        StatefulLazyVStack(
            lazyPagingItems: lazyPagingItems,
            loadingContent: { PoolGamblerBetFakeList() },
            loadingContentOnConcatenate: {
                PoolGamblerBetFakeItem()
                Divider()
            }
        ) { poolGamblerBet in
            PoolGamblerBetItemView(
                viewModel: PoolGamblerBetItemViewModel(
                    poolGamblerBet: poolGamblerBet,
                    betUseCase: diResolver.resolve(BetUseCase.self)!
                )
            )
            .padding([.horizontal], boxSpacing.medium)
            .padding([.vertical], boxSpacing.small)

            Divider()
        }
    }
}

struct PoolGamblerBetFakeList : View {
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        ForEach(1...50, id: \.self) { _ in
            PoolGamblerBetFakeItem()
            Divider()
        }
    }
}

#Preview {
    PoolGamblerBetList(
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
