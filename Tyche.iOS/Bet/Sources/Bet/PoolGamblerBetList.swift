import SwiftUI
import UI
import Core
import DataBet

struct PoolGamblerBetList: View {
    var lazyPager: LazyPager<String, PoolGamblerBetModel>
    
    @EnvironmentObject private var diResolver: DIResolver
    
    var body: some View {
        let _ = Self._printChanges()
        
        PagingVStack(
            lazyPager: lazyPager,
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
            Divider()
        }
    }
}

struct PoolGamblerBetFakeList : View {
    var body: some View {
        LazyVStack(spacing: 8) {
            ForEach(1...50, id: \.self) { _ in
                PoolGamblerBetFakeItem()
                Divider()
            }
        }
    }
}

#Preview {
    PoolGamblerBetList(
        lazyPager: LazyPager(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: PoolGamblerBetPagingSource(
                    pagingQuery: { _ in .
                        success(CursorPage(items: poolGamblerBetDummyModels(), next: nil))
                    }
                )
            )
        )
    )
}
