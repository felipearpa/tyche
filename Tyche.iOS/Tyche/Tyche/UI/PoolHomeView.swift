import SwiftUI
import Swinject
import Core
import UI
import DataPool
import Pool
import DataBet
import Bet

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
    
    init(gamblerId: String, poolId: String) {
        self.gamblerId = gamblerId
        self.poolId = poolId
    }
    
    var body: some View {
        let _ = Self._printChanges()
        
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
        .navigationTitle("Pool")
    }
}

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
