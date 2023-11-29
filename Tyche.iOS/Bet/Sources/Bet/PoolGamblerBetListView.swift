import SwiftUI
import DataBet

public struct PoolGamblerBetListView: View {
    @StateObject private var viewModel: PoolGamblerBetListViewModel
    
    public init(viewModel: @autoclosure @escaping () -> PoolGamblerBetListViewModel) {
        self._viewModel = .init(wrappedValue: viewModel())
    }
    
    public var body: some View {
        let _ = Self._printChanges()
        
        PoolGamblerBetList(lazyPager: viewModel.lazyPager)
            .padding(8)
            .refreshable { viewModel.refresh() }
            .onAppearOnce { viewModel.load() }
    }
}

#Preview {
    NavigationStack {
        PoolGamblerBetListView(
            viewModel: PoolGamblerBetListViewModel(
                getPoolGamblerBetsUseCase: GetPoolGamblerBetsUseCase(
                    poolGamblerBetRepository: PoolGamblerBetFakeRepository()
                ),
                gamblerId: "gambler-id",
                poolId: "pool-id"
            )
        )
    }.environmentObject(diFakeResolver())
}
