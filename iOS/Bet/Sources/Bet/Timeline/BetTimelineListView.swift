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
            gamblerUsername: gamblerUsername,
            onMatchOpen: onMatchOpen
        )
        .navigationTitle(.betTimelineViewTitle)
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct BetTimelineListContent: View {
    @StateObject private var viewModel: BetsTimelineViewModel
    private let onMatchOpen: MatchOpenHandler?
    private let gamblerUsername: String
    @Environment(\.boxSpacing) private var boxSpacing

    init(
        viewModel: @autoclosure @escaping () -> BetsTimelineViewModel,
        gamblerUsername: String,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.gamblerUsername = gamblerUsername
        self.onMatchOpen = onMatchOpen
    }

    var body: some View {
        let _ = Self._printChangesIfDebug()

        VStack(spacing: boxSpacing.medium) {
            Text(gamblerUsername)
                .font(.title2)
                .fontWeight(.black)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.bottom, boxSpacing.medium)

            BetTimelineList(
                lazyPagingItems: viewModel.lazyPager,
                onMatchOpen: onMatchOpen
            )
        }
        .refreshable { viewModel.refresh() }
        .onAppearOnce { viewModel.refresh() }
        .padding(boxSpacing.medium)
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
