import SwiftUI
import DataBet

public struct LiveBetListView: View {
    @StateObject private var viewModel: LiveBetListViewModel

    public init(viewModel: @autoclosure @escaping () -> LiveBetListViewModel) {
        self._viewModel = .init(wrappedValue: viewModel())
    }

    public var body: some View {
        let _ = Self._printChangesIfDebug()

        LiveBetList(lazyPagingItems: viewModel.lazyPager)
    }
}

#Preview {
    NavigationStack {
        LiveBetListView(
            viewModel: LiveBetListViewModel(
                getLivePoolGamblerBetsUseCase: GetLivePoolGamblerBetsUseCase(
                    poolGamblerBetRepository: PoolGamblerBetFakeRepository()
                ),
                gamblerId: "gambler-id",
                poolId: "pool-id"
            )
        )
    }
}
