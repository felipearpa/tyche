import SwiftUI
import Swinject
import Core
import UI
import Pool
import Bet

private let defaultDebounceTimeInMilliseconds = 700

private enum Tab: Int {
    case gamblerScores
    case bets
}

struct PoolHomeView: View {
    let diResolver: DIResolver
    let gamblerId: String
    let poolId: String
    
    @StateObject private var searchDebounceText = DebounceString(dueTime: .milliseconds(defaultDebounceTimeInMilliseconds))
    @State private var selectedTab = Tab.gamblerScores
    
    init(diResolver: DIResolver, gamblerId: String, poolId: String) {
        self.diResolver = diResolver
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
                    getPoolGamblerBetsUseCase: GetPoolGamblerBetsUseCase(
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
        }
        .navigationTitle("Pool")
        .searchable(
            text: $searchDebounceText.text,
            placement: .navigationBarDrawer(displayMode: .automatic),
            prompt: String(sharedResource: .searchingLabel)
        )
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
            diResolver: DIResolver(
                resolver: Assembler([PoolHomeAssembler()]).resolver
            ),
            gamblerId: "gambler-id",
            poolId: "pool-id"
        )
    }
}
