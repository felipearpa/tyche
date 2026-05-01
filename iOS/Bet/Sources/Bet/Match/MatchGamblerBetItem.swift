import SwiftUI
import UI

struct MatchGamblerBetItem: View {
    let poolGamblerBet: PoolGamblerBetModel
    let onTap: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)?

    @Environment(\.boxSpacing) private var boxSpacing

    init(
        poolGamblerBet: PoolGamblerBetModel,
        onTap: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)? = nil
    ) {
        self.poolGamblerBet = poolGamblerBet
        self.onTap = onTap
    }

    var body: some View {
        let row = rowContent
        if let onTap {
            Button(action: {
                onTap(
                    poolGamblerBet.poolId,
                    poolGamblerBet.gamblerId,
                    poolGamblerBet.gamblerUsername
                )
            }) {
                row
            }
            .buttonStyle(.plain)
        } else {
            row
        }
    }

    private var rowContent: some View {
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
