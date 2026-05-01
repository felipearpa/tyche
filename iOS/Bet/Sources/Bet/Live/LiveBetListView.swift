import SwiftUI
import DataBet

public struct LiveBetListView: View {
    @StateObject private var viewModel: LiveBetListViewModel
    private let onMatchOpen: MatchOpenHandler?

    public init(
        viewModel: @autoclosure @escaping () -> LiveBetListViewModel,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onMatchOpen = onMatchOpen
    }

    public var body: some View {
        let _ = Self._printChangesIfDebug()

        LiveBetList(lazyPagingItems: viewModel.lazyPager, onMatchOpen: onMatchOpen)
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
