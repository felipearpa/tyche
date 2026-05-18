import SwiftUI
import Core
import UI

struct FinishedBetItem: View {
    let poolGamblerBet: PoolGamblerBetModel

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
            Text(poolGamblerBet.matchDateTime.toShortTimeString())
                .font(.caption)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity, alignment: .leading)

            HStack {
                FlagImage(teamCode: poolGamblerBet.homeTeamId)
                    .frame(width: flagSize, height: flagSize)

                Text(poolGamblerBet.homeTeamName)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Text(poolGamblerBet.homeTeamMatchRawValue())
                    .font(.title2)
                    .multilineTextAlignment(.center)
                    .scoreWidth()

                Text(poolGamblerBet.homeTeamBetRawValue())
                    .font(.caption)
                    .multilineTextAlignment(.center)
                    .scoreWidth()
            }

            HStack {
                FlagImage(teamCode: poolGamblerBet.awayTeamId)
                    .frame(width: flagSize, height: flagSize)

                Text(poolGamblerBet.awayTeamName)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Text(poolGamblerBet.awayTeamMatchRawValue())
                    .font(.title2)
                    .multilineTextAlignment(.center)
                    .scoreWidth()

                Text(poolGamblerBet.awayTeamBetRawValue())
                    .font(.caption)
                    .multilineTextAlignment(.center)
                    .scoreWidth()
            }

            Text("+\(poolGamblerBet.score.map { String($0) } ?? "")".excludeLocalize)
                .font(.largeTitle)
                .frame(maxWidth: .infinity, alignment: .trailing)
        }
    }
}

private let flagSize: CGFloat = 24

#Preview {
    FinishedBetItem(poolGamblerBet: poolGamblerBetDummyModel())
}

#Preview("Placeholder") {
    FinishedBetItem(poolGamblerBet: poolGamblerBetPlaceholderModel(isLocked: true, isComputed: true))
        .shimmer()
}
