import SwiftUI
import DataBet

public typealias MatchOpenHandler = (
    _ poolId: String,
    _ matchId: String,
    _ homeTeamName: String,
    _ awayTeamName: String,
    _ matchDateTime: Date,
    _ homeTeamScore: Int?,
    _ awayTeamScore: Int?,
    _ isLive: Bool
) -> Void

public struct PendingBetListView: View {
    @StateObject private var viewModel: PendingBetListViewModel
    private let onMatchOpen: MatchOpenHandler?
    @Environment(\.boxSpacing) private var boxSpacing

    public init(
        viewModel: @autoclosure @escaping () -> PendingBetListViewModel,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onMatchOpen = onMatchOpen
    }

    public var body: some View {
        let _ = Self._printChangesIfDebug()

        PendingBetList(
            lazyPagingItems: viewModel.lazyPager,
            onMatchOpen: onMatchOpen
        )
        .padding(boxSpacing.medium)
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
