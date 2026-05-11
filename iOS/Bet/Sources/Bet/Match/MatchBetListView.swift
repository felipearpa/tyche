import SwiftUI
import Core
import UI
import DataBet
import ViewingState

public struct MatchBetListView: View {
    let poolId: String
    let gamblerId: String
    let matchId: String
    let onHome: (() -> Void)?
    let onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)?

    @Environment(\.diResolver) var diResolver: DIResolver
    @Environment(\.boxSpacing) private var boxSpacing

    public init(
        poolId: String,
        gamblerId: String,
        matchId: String,
        onHome: (() -> Void)? = nil,
        onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)? = nil
    ) {
        self.poolId = poolId
        self.gamblerId = gamblerId
        self.matchId = matchId
        self.onHome = onHome
        self.onGamblerOpen = onGamblerOpen
    }

    public var body: some View {
        let _ = Self._printChangesIfDebug()

        MatchBetListContent(
            viewModel: MatchBetListViewModel(
                getPoolGamblerBetUseCase: GetPoolGamblerBetUseCase(
                    poolGamblerBetRepository: diResolver.resolve(PoolGamblerBetRepository.self)!
                ),
                getPoolMatchGamblerBetsUseCase: GetPoolMatchGamblerBetsUseCase(
                    poolGamblerBetRepository: diResolver.resolve(PoolGamblerBetRepository.self)!
                ),
                poolId: poolId,
                gamblerId: gamblerId,
                matchId: matchId
            ),
            onGamblerOpen: onGamblerOpen
        )
        .navigationTitle(String(localized: .matchBetsViewTitle))
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

private struct MatchBetListContent: View {
    @StateObject private var viewModel: MatchBetListViewModel
    let onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)?

    @Environment(\.boxSpacing) private var boxSpacing

    init(
        viewModel: @autoclosure @escaping () -> MatchBetListViewModel,
        onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)? = nil
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onGamblerOpen = onGamblerOpen
    }

    var body: some View {
        let _ = Self._printChangesIfDebug()

        Group {
            switch viewModel.poolGamblerBetState {
            case .idle, .loading:
                VStack(spacing: boxSpacing.medium) {
                    MatchHeaderPlaceholderItem()
                        .padding(.horizontal, boxSpacing.medium)

                    MatchBetList(lazyPagingItems: viewModel.lazyPager, onGamblerOpen: onGamblerOpen)
                }
                .padding(.vertical, boxSpacing.medium)

            case .failure(let error):
                FailureContent(
                    localizedError: error.localizedErrorOrDefault(),
                    onRetry: { Task { await viewModel.loadPoolGamblerBet() } }
                )

            case .loaded(let bet):
                VStack(spacing: boxSpacing.medium) {
                    MatchHeader(bet: bet)
                        .padding(.horizontal, boxSpacing.medium)

                    if bet.isPending {
                        PredictionsOpenContent()
                    } else {
                        MatchBetList(lazyPagingItems: viewModel.lazyPager, onGamblerOpen: onGamblerOpen)
                            .refreshable {
                                await viewModel.loadPoolGamblerBet()
                                viewModel.refresh()
                            }
                    }
                }
                .padding(.vertical, boxSpacing.medium)
            }
        }
        .task { await viewModel.loadPoolGamblerBet() }
    }
}

private struct FailureContent: View {
    let localizedError: LocalizedError
    let onRetry: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.large) {
            ErrorView(localizedError: localizedError)

            Button(action: onRetry) {
                Text(sharedResource: .retryAction)
            }
        }
        .padding(.all, boxSpacing.large)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct PredictionsOpenContent: View {
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.large) {
            Image(systemName: "lock.fill")
                .resizable()
                .scaledToFit()
                .frame(width: 64, height: 64)
                .foregroundStyle(Color.accentColor)

            VStack(spacing: boxSpacing.medium) {
                Text(.predictionsOpenTitle)
                    .font(.headline)
                    .multilineTextAlignment(.center)

                Text(.predictionsOpenMessage)
                    .font(.body)
                    .foregroundStyle(Color.secondary)
                    .multilineTextAlignment(.center)
            }
        }
        .padding(.all, boxSpacing.large)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

#Preview {
    NavigationStack {
        MatchBetListView(
            poolId: "pool-id",
            gamblerId: "gambler-id",
            matchId: "match-id"
        )
        .environment(\.diResolver, diFakeResolver())
    }
}
