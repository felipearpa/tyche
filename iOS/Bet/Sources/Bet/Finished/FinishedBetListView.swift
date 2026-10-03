import SwiftUI
import DataBet
import DataPool

public struct FinishedBetListView : View {
    @StateObject private var viewModel: FinishedBetListViewModel
    private let onMatchOpen: MatchOpenHandler?

    /// True while a pull to refresh is in progress; its own indicator then covers the total, so
    /// the summary shows no second progress indicator.
    @State private var isPullRefreshing = false

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
            pointsSummary: viewModel.pointsSummary,
            showsSummaryRefreshProgress: !isPullRefreshing,
            onPointsSummaryRetry: { Task { await viewModel.loadPointsSummary() } },
            onMatchOpen: onMatchOpen
        )
        .refreshable {
            isPullRefreshing = true
            await viewModel.refreshListAndPointsSummary()
            isPullRefreshing = false
        }
        .onAppearOnce {
            viewModel.refresh()
            Task { await viewModel.loadPointsSummary() }
        }
    }
}

#Preview {
    NavigationStack {
        FinishedBetListView(
            viewModel: FinishedBetListViewModel(
                getFinishedPoolGamblerBetsUseCase: GetFinishedPoolGamblerBetsUseCase(
                    poolGamblerBetRepository: PoolGamblerBetFakeRepository()
                ),
                getPoolGamblerScoreUseCase: GetPoolGamblerScoreUseCase(
                    poolGamblerScoreRepository: PoolGamblerScoreFakeRepository()
                ),
                gamblerId: "gambler-id",
                poolId: "pool-id"
            )
        )
        .navigationTitle("History")
    }
}
