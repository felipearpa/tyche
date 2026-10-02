import SwiftUI
import UI

/// One gambler's bet and points for a match. Loading slots render this same component from a
/// placeholder model with `isPlaceholder: true`: native redaction under the shared
/// `LoadingPlaceholderPulse` conceals its content, and it ignores touches and stays out of the
/// accessibility tree.
struct MatchGamblerBetItem: View {
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
        HStack(spacing: boxSpacing.medium) {
            Text(poolGamblerBet.gamblerUsername)
                .font(.body)
                .lineLimit(1)
                .truncationMode(.tail)
                .frame(maxWidth: .infinity, alignment: .leading)

            if poolGamblerBet.betScore != nil {
                Text("\(poolGamblerBet.homeTeamBetRawValue()) - \(poolGamblerBet.awayTeamBetRawValue())".excludeLocalize)
                    .font(.headline)
                    .multilineTextAlignment(.center)
            }

            Spacer().frame(width: boxSpacing.medium)

            Text(poolGamblerBet.score.map { "+\($0)".excludeLocalize } ?? "".excludeLocalize)
                .font(.title2)
                .multilineTextAlignment(.trailing)
        }
    }
}

#Preview {
    MatchGamblerBetItem(poolGamblerBet: poolGamblerBetDummyModel())
}

#Preview("Placeholder") {
    MatchGamblerBetItem(poolGamblerBet: poolGamblerBetPlaceholderModel(), isPlaceholder: true)
}
