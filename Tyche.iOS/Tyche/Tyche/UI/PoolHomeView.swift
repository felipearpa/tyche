import SwiftUI
import Swinject
import Core
import Pool
import UI

private let defaultDebounceTimeInMilliseconds = 700

struct PoolHomeView: View {
    let diResolver: DIResolver
    let gamblerId: String?
    let poolId: String
    @StateObject private var searchDebounceText = DebounceString(dueTime: .milliseconds(defaultDebounceTimeInMilliseconds))
    
    init(diResolver: DIResolver, gamblerId: String?, poolId: String) {
        self.diResolver = diResolver
        self.gamblerId = gamblerId
        self.poolId = poolId
    }
    
    var body: some View {
        TabView {
            GamblerScoreListView(
                viewModel: GamblerScoreListViewModel(
                    getPoolGamblerScoresByPoolUseCase: GetPoolGamblerScoresByPoolUseCase(
                        poolGamblerScoreRepository: diResolver.resolve(PoolGamblerScoreRepository.self)!
                    ),
                    gamblerId: gamblerId,
                    poolId: poolId
                )
            )
            .tabItem {
                Label(StringScheme.scoreTab.localizedKey,
                      image: ResourceScheme.sportScore.name)
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
            diResolver: DIResolver(
                resolver: Assembler([PoolHomeAssembler()]).resolver
            ),
            gamblerId: nil,
            poolId: "pool-id"
        )
    }
}
