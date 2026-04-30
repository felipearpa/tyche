import SwiftUI
import Swinject
import Core
import DataBet
import Bet

struct GamblerBetsView: View {
    let poolId: String
    let gamblerId: String
    let gamblerUsername: String
    let onMatchOpen: MatchOpenHandler?

    @Environment(\.diResolver) var diResolver: DIResolver

    init(
        poolId: String,
        gamblerId: String,
        gamblerUsername: String,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self.poolId = poolId
        self.gamblerId = gamblerId
        self.gamblerUsername = gamblerUsername
        self.onMatchOpen = onMatchOpen
    }

    var body: some View {
        let _ = Self._printChangesIfDebug()

        BetsTimelineListView(
            viewModel: BetsTimelineViewModel(
                getGamblerBetsTimelineUseCase: GetGamblerBetsTimelineUseCase(
                    poolGamblerBetRepository: diResolver.resolve(PoolGamblerBetRepository.self)!
                ),
                poolId: poolId,
                gamblerId: gamblerId
            ),
            onMatchOpen: onMatchOpen
        )
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
