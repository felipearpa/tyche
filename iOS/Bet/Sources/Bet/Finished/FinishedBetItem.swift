import SwiftUI
import Core
import UI

/// A finished match: the result, the gambler's bet, and the points earned. Loading slots
/// render this same component from a placeholder model with `isPlaceholder: true`: native
/// redaction under the shared `LoadingPlaceholderPulse` conceals its content, and it ignores
/// touches and stays out of the accessibility tree.
struct FinishedBetItem: View {
    let poolGamblerBet: PoolGamblerBetModel
    let isPlaceholder: Bool

    @Environment(\.boxSpacing) private var boxSpacing

    init(poolGamblerBet: PoolGamblerBetModel, isPlaceholder: Bool = false) {
        self.poolGamblerBet = poolGamblerBet
        self.isPlaceholder = isPlaceholder
    }

    var body: some View {
        Group {
            if isPlaceholder {
                PulsingPlaceholderContent { content }
            } else {
                content
            }
        }
        .allowsHitTesting(!isPlaceholder)
        .accessibilityHidden(isPlaceholder)
    }

    private var content: some View {
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
    FinishedBetItem(
        poolGamblerBet: poolGamblerBetPlaceholderModel(isLocked: true, isComputed: true),
        isPlaceholder: true
    )
}
