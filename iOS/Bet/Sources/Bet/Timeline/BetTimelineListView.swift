import SwiftUI
import Core
import DataBet

public struct BetTimelineListView: View {
    let poolId: String
    let gamblerId: String
    let gamblerUsername: String
    let onMatchOpen: MatchOpenHandler?

    @Environment(\.diResolver) var diResolver: DIResolver

    public init(
        poolId: String,
        gamblerId: String,
        gamblerUsername: String,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self.poolId = poolId
        self.gamblerId = gamblerId
        self.gamblerUsername = gamblerUsername
        self.onMatchOpen = onMatchOpen
    }

    public var body: some View {
        let _ = Self._printChangesIfDebug()

        BetTimelineListContent(
            viewModel: BetsTimelineViewModel(
                getGamblerBetsTimelineUseCase: GetGamblerBetsTimelineUseCase(
                    poolGamblerBetRepository: diResolver.resolve(PoolGamblerBetRepository.self)!
                ),
                poolId: poolId,
                gamblerId: gamblerId
            ),
            onMatchOpen: onMatchOpen
        )
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                Text(gamblerUsername)
                    .font(.footnote)
                    .lineLimit(1)
                    .truncationMode(.tail)
            }
        }
    }
}

private struct BetTimelineListContent: View {
    @StateObject private var viewModel: BetsTimelineViewModel
    private let onMatchOpen: MatchOpenHandler?

    init(
        viewModel: @autoclosure @escaping () -> BetsTimelineViewModel,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onMatchOpen = onMatchOpen
    }

    var body: some View {
        let _ = Self._printChangesIfDebug()

        BetsTimelineList(
            lazyPagingItems: viewModel.lazyPager,
            onMatchOpen: onMatchOpen
        )
        .refreshable { viewModel.refresh() }
        .onAppearOnce { viewModel.refresh() }
    }
}

#Preview {
    NavigationStack {
        BetTimelineListView(
            poolId: "pool-id",
            gamblerId: "gambler-id",
            gamblerUsername: "felipearcila@gmail.com"
        )
        .environment(\.diResolver, diFakeResolver())
    }
}
