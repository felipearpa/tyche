import SwiftUI
import UI

struct MatchGamblerBetItem: View {
    let poolGamblerBet: PoolGamblerBetModel

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        HStack(spacing: boxSpacing.medium) {
            Text(poolGamblerBet.gamblerUsername)
                .font(.body)
                .lineLimit(1)
                .truncationMode(.tail)
                .frame(maxWidth: .infinity, alignment: .leading)

            if poolGamblerBet.betScore != nil {
                Text("\(poolGamblerBet.homeTeamBetRawValue()) - \(poolGamblerBet.awayTeamBetRawValue())")
                    .font(.headline)
                    .multilineTextAlignment(.center)
            }

            Spacer().frame(width: boxSpacing.medium)

            Text(poolGamblerBet.score.map { "+\($0)" } ?? "")
                .font(.title2)
                .multilineTextAlignment(.trailing)
        }
    }
}

struct MatchHeaderPlaceholderItem: View {
    var body: some View {
        MatchHeader(
            bet: poolGamblerBetFakeModel(isLocked: false, isComputed: false)
        )
        .shimmer()
    }
}

#Preview {
    MatchGamblerBetItem(poolGamblerBet: poolGamblerBetDummyModel())
}

#Preview {
    MatchHeaderPlaceholderItem()
}
