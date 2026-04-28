import SwiftUI
import Swinject
import Core
import DataBet
import Bet

struct GamblerBetsView: View {
    let poolId: String
    let gamblerId: String
    let gamblerUsername: String

    @Environment(\.diResolver) var diResolver: DIResolver
    @State private var selectedTab = GamblerBetsTab.live

    var body: some View {
        let _ = Self._printChangesIfDebug()

        TabView(selection: $selectedTab) {
            LiveBetListView(
                viewModel: LiveBetListViewModel(
                    getLivePoolGamblerBetsUseCase: GetLivePoolGamblerBetsUseCase(
                        poolGamblerBetRepository: diResolver.resolve(PoolGamblerBetRepository.self)!
                    ),
                    gamblerId: gamblerId,
                    poolId: poolId
                )
            )
            .tag(GamblerBetsTab.live)
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
            .tag(GamblerBetsTab.history)
            .tabItem {
                Label(
                    title: { Text(String(.historyBetsTab)) },
                    icon: { Image(.money) }
                )
            }
        }
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

private enum GamblerBetsTab: Int {
    case live
    case history
}

#Preview {
    var diResolver = DIResolver(
        resolver: Assembler([
            GamblerBetsAssembler()
        ]).resolver)

    NavigationStack {
        GamblerBetsView(
            poolId: "pool-id",
            gamblerId: "gambler-id",
            gamblerUsername: "felipearcila@gmail.com"
        )
        .environment(\.diResolver, diResolver)
    }
}

private class GamblerBetsAssembler: Assembly {
    func assemble(container: Container) {
        container.register(PoolGamblerBetRepository.self) { _ in
            PoolGamblerBetFakeRepository()
        }
    }
}
