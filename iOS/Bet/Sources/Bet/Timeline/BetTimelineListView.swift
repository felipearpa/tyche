import SwiftUI
import Core
import UI
import DataBet
import DataPool

public struct BetTimelineListView: View {
    let poolId: String
    let gamblerId: String
    let gamblerUsername: String
    let onHome: (() -> Void)?
    let onMatchOpen: MatchOpenHandler?

    @Environment(\.diResolver) var diResolver: DIResolver

    public init(
        poolId: String,
        gamblerId: String,
        gamblerUsername: String,
        onHome: (() -> Void)? = nil,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self.poolId = poolId
        self.gamblerId = gamblerId
        self.gamblerUsername = gamblerUsername
        self.onHome = onHome
        self.onMatchOpen = onMatchOpen
    }

    public var body: some View {
        let _ = Self._printChangesIfDebug()

        BetTimelineListContent(
            viewModel: BetTimelineListViewModel(
                getGamblerBetsTimelineUseCase: GetGamblerBetsTimelineUseCase(
                    poolGamblerBetRepository: diResolver.resolve(PoolGamblerBetRepository.self)!
                ),
                getPoolGamblerScoreUseCase: diResolver.resolve(GetPoolGamblerScoreUseCase.self)!,
                poolId: poolId,
                gamblerId: gamblerId
            ),
            gamblerId: gamblerId,
            gamblerUsername: gamblerUsername,
            onMatchOpen: onMatchOpen
        )
        .navigationTitle(.betTimelineViewTitle)
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarItems(trailing: navigationBarTrailing())
    }

    @ViewBuilder
    private func navigationBarTrailing() -> some View {
        if let onHome {
            Button(action: onHome) {
                Image(sharedResource: .home)
                    .resizable()
                    .scaledToFit()
                    .frame(width: HOME_ICON_SIZE, height: HOME_ICON_SIZE)
                    .tint(.primary)
            }
            .accessibilityLabel(Text(sharedResource: .goHomeAction))
        }
    }
}

private let HOME_ICON_SIZE: CGFloat = 24

private struct BetTimelineListContent: View {
    @StateObject private var viewModel: BetTimelineListViewModel
    private let gamblerId: String
    private let gamblerUsername: String
    private let onMatchOpen: MatchOpenHandler?

    /// True while a pull to refresh is in progress; its own indicator then covers the total, so
    /// the summary shows no second progress indicator.
    @State private var isPullRefreshing = false

    init(
        viewModel: @autoclosure @escaping () -> BetTimelineListViewModel,
        gamblerId: String,
        gamblerUsername: String,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.gamblerId = gamblerId
        self.gamblerUsername = gamblerUsername
        self.onMatchOpen = onMatchOpen
    }

    var body: some View {
        let _ = Self._printChangesIfDebug()

        BetTimelineList(
            lazyPagingItems: viewModel.lazyPager,
            gamblerId: gamblerId,
            gamblerUsername: gamblerUsername,
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
        BetTimelineListView(
            poolId: "pool-id",
            gamblerId: "gambler-id",
            gamblerUsername: "El mono"
        )
        .environment(\.diResolver, diFakeResolver())
    }
}
