import SwiftUI
import Swinject
import Core
import UI
import DataPool
import Pool
import DataBet
import Bet
import Session

private enum Tab: Int {
    case gamblerScores
    case bets
    case historyBet
}

struct PoolHomeView: View {
    let gamblerId: String
    let poolId: String
    
    @Environment(\.diResolver) var diResolver: DIResolver
    @State private var selectedTab = Tab.gamblerScores
    @State private var drawerVisible = false

    init(gamblerId: String, poolId: String) {
        self.gamblerId = gamblerId
        self.poolId = poolId
    }
    
    var body: some View {
        TabView(selection: $selectedTab) {
            GamblerScoreListView(
                viewModel: GamblerScoreListViewModel(
                    getPoolGamblerScoresByPoolUseCase: GetPoolGamblerScoresByPoolUseCase(
                        poolGamblerScoreRepository: diResolver.resolve(PoolGamblerScoreRepository.self)!
                    ),
                    gamblerId: gamblerId,
                    poolId: poolId
                )
            )
            .tag(Tab.gamblerScores)
            .tabItem {
                Label(
                    title: { Text(String(.scoreTab)) },
                    icon: { Image(.sportScore) }
                )
            }
            
            PoolGamblerBetListView(
                viewModel: PoolGamblerBetListViewModel(
                    getPoolGamblerBetsUseCase: GetPendingPoolGamblerBetsUseCase(
                        poolGamblerBetRepository: diResolver.resolve(PoolGamblerBetRepository.self)!
                    ),
                    gamblerId: gamblerId,
                    poolId: poolId
                )
            )
            .tag(Tab.bets)
            .tabItem {
                Label(
                    title: { Text(String(.betTab)) },
                    icon: { Image(.money) }
                )
            }

            FinishedPoolGamblerBetListView(
                viewModel: FinishedBetListViewModel(
                    getFinishedPoolGamblerBetsUseCase: GetFinishedPoolGamblerBetsUseCase(
                        poolGamblerBetRepository: diResolver.resolve(PoolGamblerBetRepository.self)!
                    ),
                    gamblerId: gamblerId,
                    poolId: poolId,
                )
            )
            .tag(Tab.historyBet)
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
                changePool: {},
                onLogout: {}
            )
        }
        .navigationTitle("Pool")
        .navigationBarItems(leading: navigationBarLeading())
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

private let ICON_SIZE: CGFloat = 24

struct PoolHomeView_Previews: PreviewProvider {
    private class PoolHomeAssembler: Assembly {
        func assemble(container: Container) {
            container.register(PoolGamblerScoreRepository.self) { _ in
                PoolGamblerScoreFakeRepository()
            }
        }
    }
    
    static var previews: some View {
        PoolHomeView(
            gamblerId: "gambler-id",
            poolId: "pool-id"
        )
    }
}
