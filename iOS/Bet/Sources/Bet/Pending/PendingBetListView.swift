import SwiftUI
import DataBet

public struct PendingBetListView: View {
    @StateObject private var viewModel: PendingBetListViewModel
    
    public init(viewModel: @autoclosure @escaping () -> PendingBetListViewModel) {
        self._viewModel = .init(wrappedValue: viewModel())
    }
    
    public var body: some View {
        let _ = Self._printChangesIfDebug()
        
        PendingBetList(lazyPagingItems: viewModel.lazyPager)
    }
}

#Preview {
    NavigationStack {
        PendingBetListView(
            viewModel: PendingBetListViewModel(
                getPoolGamblerBetsUseCase: GetPendingPoolGamblerBetsUseCase(
                    poolGamblerBetRepository: PoolGamblerBetFakeRepository()
                ),
                gamblerId: "gambler-id",
                poolId: "pool-id"
            )
        )
    }
}
