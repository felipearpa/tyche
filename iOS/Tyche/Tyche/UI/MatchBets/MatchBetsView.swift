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

    var body: some View {
        let _ = Self._printChangesIfDebug()

        VStack(alignment: .leading, spacing: 0) {
            MatchHeader(
                homeTeamName: homeTeamName,
                awayTeamName: awayTeamName,
                matchDateTime: matchDateTime,
                homeTeamScore: homeTeamScore,
                awayTeamScore: awayTeamScore
            )

            Divider()

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
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct MatchHeader: View {
    let homeTeamName: String
    let awayTeamName: String
    let matchDateTime: Date
    let homeTeamScore: Int?
    let awayTeamScore: Int?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(homeTeamName)
                    .font(.title3)
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .frame(maxWidth: .infinity, alignment: .leading)

                if let home = homeTeamScore, let away = awayTeamScore {
                    Text("\(home) - \(away)").font(.title2)
                } else {
                    Text("-").font(.title2)
                }

                Text(awayTeamName)
                    .font(.title3)
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }

            Text(matchDateTime, style: .date)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(.horizontal)
        .padding(.vertical, 12)
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
