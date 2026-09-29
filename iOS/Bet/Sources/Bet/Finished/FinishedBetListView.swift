import SwiftUI
import DataBet

public struct FinishedBetListView : View {
    @StateObject private var viewModel: FinishedBetListViewModel
    private let onMatchOpen: MatchOpenHandler?
    @Environment(\.boxSpacing) private var boxSpacing

    public init(
        viewModel: @autoclosure @escaping () -> FinishedBetListViewModel,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onMatchOpen = onMatchOpen
    }

    public var body: some View {
        let _ = Self._printChangesIfDebug()

        FinishedBetList(
            lazyPagingItems: viewModel.lazyPager,
            onMatchOpen: onMatchOpen
        )
        .refreshable { viewModel.refresh() }
        .onAppearOnce { viewModel.refresh() }
        // The top spacing stays outside the scroll view, keeping rows out from under the
        // navigation bar: its large title does not collapse on every pool-home tab.
        .padding(.top, boxSpacing.medium)
    }
}

#Preview {
    NavigationStack {
        FinishedBetListView(
            viewModel: FinishedBetListViewModel(
                getFinishedPoolGamblerBetsUseCase: GetFinishedPoolGamblerBetsUseCase(
                    poolGamblerBetRepository: PoolGamblerBetFakeRepository()
                ),
                gamblerId: "gambler-id",
                poolId: "pool-id"
            )
        )
    }
}
