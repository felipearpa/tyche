import SwiftUI
import Core
import DataBet

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
                poolId: poolId,
                gamblerId: gamblerId
            ),
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
        }
    }
}

private let HOME_ICON_SIZE: CGFloat = 24

private struct BetTimelineListContent: View {
    @StateObject private var viewModel: BetTimelineListViewModel
    private let onMatchOpen: MatchOpenHandler?
    private let gamblerUsername: String
    @Environment(\.boxSpacing) private var boxSpacing

    init(
        viewModel: @autoclosure @escaping () -> BetTimelineListViewModel,
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
