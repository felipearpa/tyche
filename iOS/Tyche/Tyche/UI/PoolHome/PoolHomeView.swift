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

struct PoolHomeView: View {
    let gamblerId: String
    let poolId: String
    let onChangePool: () -> Void
    let onMenuTap: () -> Void
    let onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)?
    let onMatchOpen: MatchOpenHandler?

    @Environment(\.diResolver) private var diResolver: DIResolver
    @State private var selectedTab = PoolHomeTab.gamblerScores

    var body: some View {
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
                    title: { Text(.scoreTab) },
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
                    title: { Text(.betTab) },
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
                    title: { Text(.historyBetsTab) },
                    icon: { Image(.money) }
                )
            }
        }
        .navigationTitle(selectedTab.title)
        .navigationBarItems(
            leading: navigationBarLeading(),
            trailing: navigationBarTrailing()
        )
    }

    private func navigationBarLeading() -> some View {
        Button(action: onMenuTap) {
            AutoEmailAvatar()
                .navigationEmailAvatar()
        }
        .buttonStyle(.plain)
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
        onChangePool: {},
        onMenuTap: {},
        onGamblerOpen: nil,
        onMatchOpen: nil
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
