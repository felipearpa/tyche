import SwiftUI
import Swinject
import Core
import DataBet
import Bet

struct MatchBetsView: View {
    let poolId: String
    let matchId: String
    let homeTeamName: String
    let awayTeamName: String
    let matchDateTime: Date
    let homeTeamScore: Int?
    let awayTeamScore: Int?

    @Environment(\.diResolver) var diResolver: DIResolver
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        let _ = Self._printChangesIfDebug()

        VStack(spacing: boxSpacing.medium) {
            MatchHeader(
                homeTeamName: homeTeamName,
                awayTeamName: awayTeamName,
                matchDateTime: matchDateTime,
                homeTeamScore: homeTeamScore,
                awayTeamScore: awayTeamScore
            )

            MatchBetListView(
                viewModel: MatchBetListViewModel(
                    getPoolMatchGamblerBetsUseCase: GetPoolMatchGamblerBetsUseCase(
                        poolGamblerBetRepository: diResolver.resolve(PoolGamblerBetRepository.self)!
                    ),
                    poolId: poolId,
                    matchId: matchId
                )
            )
        }
        .padding(boxSpacing.medium)
        .navigationTitle(String(.matchBetsViewTitle))
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct MatchHeader: View {
    let homeTeamName: String
    let awayTeamName: String
    let matchDateTime: Date
    let homeTeamScore: Int?
    let awayTeamScore: Int?

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(alignment: .center, spacing: boxSpacing.small) {
            HStack(spacing: boxSpacing.medium) {
                Text(homeTeamName)
                    .font(.headline)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .truncationMode(.tail)
                    .frame(maxWidth: .infinity)

                Text(homeTeamScore.map { String($0) } ?? "")
                    .font(.title2)

                Text("-")

                Text(awayTeamScore.map { String($0) } ?? "")
                    .font(.title2)

                Text(awayTeamName)
                    .font(.headline)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .truncationMode(.tail)
                    .frame(maxWidth: .infinity)
            }

            Text(matchDateTime.toShortDateTimeString())
                .font(.caption)
        }
        .frame(maxWidth: .infinity)
    }
}

#Preview {
    var diResolver = DIResolver(
        resolver: Assembler([
            MatchBetsAssembler()
        ]).resolver)

    NavigationStack {
        MatchBetsView(
            poolId: "pool-id",
            matchId: "match-id",
            homeTeamName: "Home FC",
            awayTeamName: "Away United",
            matchDateTime: Date(),
            homeTeamScore: 2,
            awayTeamScore: 1
        )
        .environment(\.diResolver, diResolver)
    }
}

private class MatchBetsAssembler: Assembly {
    func assemble(container: Container) {
        container.register(PoolGamblerBetRepository.self) { _ in
            PoolGamblerBetFakeRepository()
        }
    }
}
