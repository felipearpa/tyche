import SwiftUI
import UI
import Core

struct PoolGamblerBetList: View {
    var lazyPager: LazyPager<String, PoolGamblerBetModel>
    
    @EnvironmentObject private var diResolver: DIResolver
    
    var body: some View {
        let _ = Self._printChanges()
        
        PagingVStack(
            lazyPager: lazyPager,
            loadingContent: { PoolGamblerBetFakeList() },
            loadingContentOnConcatenate: {
                PoolGamblerBetItem(poolGamblerBet: fakePoolGamblerBetModel())
                    .shimmer()
                Divider()
            }
        ) { poolGamblerBet in
            PoolGamblerBetItemView(
                viewModel: PoolGamblerBetItemViewModel(
                    poolGamblerBet: poolGamblerBet,
                    betUseCase: diResolver.resolve(BetUseCase.self)!
                )
            )
            Divider()
        }
    }
}

struct PoolGamblerBetFakeList : View {
    private let poolGamblerBets: [PoolGamblerBetModel] = (1...50).lazy.map { _ in
        fakePoolGamblerBetModel()
    }
    
    var body: some View {
        LazyVStack(spacing: 8) {
            ForEach(poolGamblerBets) { poolGamblerBet in
                PoolGamblerBetItem(poolGamblerBet: poolGamblerBet)
                    .shimmer()
                Divider()
            }
        }
    }
}

struct PoolGamblerBetList_Previews: PreviewProvider {
    static var previews: some View {
        PoolGamblerBetList(
            lazyPager: LazyPager(
                pagingData: PagingData(
                    pagingConfig: PagingConfig(prefetchDistance: 5),
                    pagingSourceFactory: PoolGamblerBetPagingSource(
                        pagingQuery: { _ in .
                            success(CursorPage(items: poolGamblerBetModels(), next: nil))
                        }
                    )
                )
            )
        )
    }
}
