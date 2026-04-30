import SwiftUI
import Swinject
import Core
import UI
import DataPool
import Pool
import DataBet
import Bet
import Session

struct PoolHomeView: View {
    let gamblerId: String
    let poolId: String
    let onChangePool: () -> Void
    let onSignOut: () -> Void
    let onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)?
    let onMatchOpen: MatchOpenHandler?

    @Environment(\.diResolver) var diResolver: DIResolver
    @State private var selectedTab = PoolHomeTab.gamblerScores
    @State private var drawerVisible = false

    init(
        gamblerId: String,
        poolId: String,
        onChangePool: @escaping () -> Void,
        onSignOut: @escaping () -> Void,
        onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)? = nil,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self.gamblerId = gamblerId
        self.poolId = poolId
        self.onChangePool = onChangePool
        self.onSignOut = onSignOut
        self.onGamblerOpen = onGamblerOpen
        self.onMatchOpen = onMatchOpen
    }

    var body: some View {
        let _ = Self._printChangesIfDebug()

        TabView(selection: $selectedTab) {
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
            .tag(PoolHomeTab.gamblerScores)
            .tabItem {
                Label(
                    title: { Text(String(.scoreTab)) },
                    icon: { Image(.sportScore) }
                )
            }

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
            .tag(PoolHomeTab.bets)
            .tabItem {
                Label(
                    title: { Text(String(.betTab)) },
                    icon: { Image(.money) }
                )
            }

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
            .tag(PoolHomeTab.historyBet)
            .tabItem {
                Label(
                    title: { Text(String(.historyBetsTab)) },
                    icon: { Image(.money) }
                )
            }
        }
        .drawer(isShowing: $drawerVisible) {
            PoolHomeDrawerView(
                viewModel: PoolHomeDrawerViewModel(
                    poolId: poolId,
                    gamblerId: gamblerId,
                    logoutUseCase: diResolver.resolve(LogOutUseCase.self)!,
                    getPoolGamblerScoreUseCase: diResolver.resolve(GetPoolGamblerScoreUseCase.self)!,
                ),
                changePool: {
                    drawerVisible = false
                    onChangePool()
                },
                onLogout: {
                    drawerVisible = false
                    onSignOut()
                }
            )
        }
        .navigationTitle(selectedTab.title)
        .navigationBarItems(leading: navigationBarLeading())
        .toolbar(drawerVisible ? .hidden : .visible, for: .navigationBar)
    }

    private func navigationBarLeading() -> some View {
        Button(action: { drawerVisible.toggle() }) {
            Image(sharedResource: .menu)
                .resizable()
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
    var title: String {
        switch self {
        case .gamblerScores:
            return String(.scoreTab)
        case .bets:
            return String(.betTab)
        case .historyBet:
            return String(.historyBetsTab)
        }
    }
}

private let ICON_SIZE: CGFloat = 24

#Preview {
    var diResolver = DIResolver(
        resolver:Assembler([
            PoolHomeAssembler()
        ]).resolver)

    PoolHomeView(
        gamblerId: "gambler-id",
        poolId: "pool-id",
        onChangePool: {},
        onSignOut: {}
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
