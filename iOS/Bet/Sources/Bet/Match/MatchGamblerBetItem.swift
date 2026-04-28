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

            Text("\(poolGamblerBet.homeTeamBetRawValue()) - \(poolGamblerBet.awayTeamBetRawValue())")
                .font(.title3)

            if let score = poolGamblerBet.score {
                Text("+\(score)")
                    .font(.title3)
            }
        }
    }
}

#Preview {
    MatchGamblerBetItem(poolGamblerBet: poolGamblerBetDummyModel())
}
