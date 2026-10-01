import SwiftUI
import Swinject
import Core
import UI
import DataPool
import Pool
import DataBet
import Bet
import Session
import Account

/// Pool home's tabs. Each tab has its own navigation stack, so its title and bars follow its own
/// list: a tab view inside one shared stack would leave the navigation bar following only the
/// first tab it showed. Destinations push onto the selected tab's stack and hide the tab bar,
/// as they covered the whole tab view before.
struct PoolHomeView<Destinations: ViewModifier>: View {
    let gamblerId: String
    let poolId: String
    /// The destinations above the selected tab. The other tabs stay at their roots, since the
    /// tab bar is hidden while a destination is shown.
    @Binding var path: NavigationPath
    let onChangePool: () -> Void
    let onMenuTap: () -> Void
    let onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)?
    let onMatchOpen: MatchOpenHandler?
    /// Registers the destinations on each tab's stack.
    let destinations: Destinations

    @Environment(\.diResolver) private var diResolver: DIResolver
    @State private var selectedTab = PoolHomeTab.gamblerScores

    var body: some View {
        TabView(selection: $selectedTab) {
            tabStack(.gamblerScores) {
                GamblerScoreListView(
                    viewModel: GamblerScoreListViewModel(
                        getPoolGamblerScoresByPoolUseCase: GetPoolGamblerScoresByPoolUseCase(
                            poolGamblerScoreRepository: diResolver.resolve(PoolGamblerScoreRepository.self)!
                        ),
                        gamblerId: gamblerId,
                        poolId: poolId
                    ),
                    onGamblerOpen: onGamblerOpen
                )
            }
            .tabItem {
                Label(
                    title: { Text(.scoreTab) },
                    icon: { Image(.sportScore) }
                )
            }

            tabStack(.bets) {
                PendingBetListView(
                    viewModel: PendingBetListViewModel(
                        getPoolGamblerBetsUseCase: GetPendingPoolGamblerBetsUseCase(
                            poolGamblerBetRepository: diResolver.resolve(PoolGamblerBetRepository.self)!
                        ),
                        gamblerId: gamblerId,
                        poolId: poolId
                    ),
                    onMatchOpen: onMatchOpen
                )
            }
            .tabItem {
                Label(
                    title: { Text(.betTab) },
                    icon: { Image(.money) }
                )
            }

            tabStack(.historyBet) {
                FinishedBetListView(
                    viewModel: FinishedBetListViewModel(
                        getFinishedPoolGamblerBetsUseCase: GetFinishedPoolGamblerBetsUseCase(
                            poolGamblerBetRepository: diResolver.resolve(PoolGamblerBetRepository.self)!
                        ),
                        gamblerId: gamblerId,
                        poolId: poolId,
                    ),
                    onMatchOpen: onMatchOpen
                )
            }
            .tabItem {
                Label(
                    title: { Text(.historyBetsTab) },
                    icon: { Image(.money) }
                )
            }
        }
        // The tab bar keeps its own drags; each tab's root marks where the bar begins.
        .excludesTabBarFromDrawerDrags()
    }

    private func tabStack(_ tab: PoolHomeTab, @ViewBuilder list: () -> some View) -> some View {
        NavigationStack(path: tab == selectedTab ? $path : .constant(NavigationPath())) {
            list()
                .navigationTitle(tab.title)
                .toolbar {
                    PlainToolbarItem(placement: .topBarLeading) {
                        navigationBarLeading()
                    }
                    ToolbarItem(placement: .topBarTrailing) {
                        navigationBarTrailing()
                    }
                }
                .modifier(destinations)
        }
        .drawerTabBarBoundary()
        .tag(tab)
    }

    private func navigationBarLeading() -> some View {
        Button(action: onMenuTap) {
            AutoEmailAvatar()
                .navigationEmailAvatar()
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(sharedResource: .openMenuAction))
        .drawerOpener()
    }

    private func navigationBarTrailing() -> some View {
        Button(action: onChangePool) {
            Image(systemName: "arrow.left.arrow.right")
                .resizable()
                .scaledToFit()
                .frame(width: ICON_SIZE, height: ICON_SIZE)
                .tint(.primary)
        }
    }
}

private enum PoolHomeTab: Int {
    case gamblerScores
    case bets
    case historyBet
}

private extension PoolHomeTab {
    var title: Text {
        switch self {
        case .gamblerScores:
            return Text(.scoreTab)
        case .bets:
            return Text(.betTab)
        case .historyBet:
            return Text(.historyBetsTab)
        }
    }
}

private let ICON_SIZE: CGFloat = 24

#Preview {
    let diResolver = DIResolver(
        resolver: Assembler([
            PoolHomeAssembler()
        ]).resolver)

    PoolHomeView(
        gamblerId: "gambler-id",
        poolId: "pool-id",
        path: .constant(NavigationPath()),
        onChangePool: {},
        onMenuTap: {},
        onGamblerOpen: nil,
        onMatchOpen: nil,
        destinations: EmptyModifier()
    )
    .environment(\.diResolver, diResolver)
}

private class PoolHomeAssembler: Assembly {
    func assemble(container: Container) {
        container.register(PoolGamblerScoreRepository.self) { _ in
            PoolGamblerScoreFakeRepository()
        }

        container.register(PoolGamblerBetRepository.self) { _ in
            PoolGamblerBetFakeRepository()
        }
    }
}
