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
            }

            if let score = poolGamblerBet.score {
                Text("+\(score)")
                    .font(.title2)
            }
        }
    }
}

#Preview {
    MatchGamblerBetItem(poolGamblerBet: poolGamblerBetDummyModel())
}
