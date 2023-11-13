import SwiftUI

public struct PoolGamblerBetListView: View {
    @StateObject private var viewModel: PoolGamblerBetListViewModel
    
    public init(viewModel: @autoclosure @escaping () -> PoolGamblerBetListViewModel) {
        self._viewModel = .init(wrappedValue: viewModel())
    }
    
    public var body: some View {
        let _ = Self._printChanges()
        
        PoolGamblerBetList(lazyPager: viewModel.lazyPager)
            .padding(8)
            .refreshable {
                viewModel.lazyPager.refresh()
            }
            .onAppearOnce {
                viewModel.lazyPager.refresh()
            }
    }
}

struct PoolGamblerBetListView_Previews: PreviewProvider {
    static var previews: some View {
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
        }
    }
}
