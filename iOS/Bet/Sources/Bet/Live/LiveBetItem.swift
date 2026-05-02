import SwiftUI
import Core
import UI

struct LiveBetItem: View {
    let poolGamblerBet: PoolGamblerBetModel

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
            Text(poolGamblerBet.matchDateTime.toShortTimeString())
                .font(.caption)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity, alignment: .leading)

            HStack {
                Text(poolGamblerBet.homeTeamName)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Text(poolGamblerBet.homeTeamBetRawValue())
                    .font(.caption)
                    .multilineTextAlignment(.center)
                    .scoreWidth()
            }

            HStack {
                Text(poolGamblerBet.awayTeamName)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Text(poolGamblerBet.awayTeamBetRawValue())
                    .font(.caption)
                    .multilineTextAlignment(.center)
                    .scoreWidth()
            }
        }
    }
}

private extension View {
    func scoreWidth() -> some View {
        let width = String(repeating: "8", count: 3)
            .widthOfString(usingFont: UIFont.preferredFont(from: .body))
        let height: CGFloat = UIFont.preferredFont(from: .body).lineHeight
        return self.frame(width: width, height: height)
    }
}

#Preview {
    LiveBetItem(poolGamblerBet: poolGamblerBetDummyModel())
}
