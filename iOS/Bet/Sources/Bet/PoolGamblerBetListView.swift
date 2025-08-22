import SwiftUI
import DataBet

public struct PoolGamblerBetListView: View {
    @StateObject private var viewModel: PoolGamblerBetListViewModel
    
    public init(viewModel: @autoclosure @escaping () -> PoolGamblerBetListViewModel) {
        self._viewModel = .init(wrappedValue: viewModel())
    }
    
    public var body: some View {
        let _ = Self._printChanges()
        
        PoolGamblerBetList(lazyPagingItems: viewModel.lazyPager)
            .refreshable { viewModel.refresh() }
            .onAppearOnce { viewModel.refresh() }
    }
}

#Preview {
    NavigationStack {
        PoolGamblerBetListView(
            viewModel: PoolGamblerBetListViewModel(
                getPoolGamblerBetsUseCase: GetPendingPoolGamblerBetsUseCase(
                    poolGamblerBetRepository: PoolGamblerBetFakeRepository()
                ),
                gamblerId: "gambler-id",
                poolId: "pool-id"
            )
        )
    }
}
