import SwiftUI
import Core
import UI

struct FinishedBetItem: View {
    let poolGamblerBet: PoolGamblerBetModel

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
            HStack(spacing: boxSpacing.small) {
                Text(poolGamblerBet.matchDateTime.toShortTimeString())
                    .font(.caption)
                    .multilineTextAlignment(.center)

                Spacer()

                Text("+\(poolGamblerBet.score.map { String($0) } ?? "")")
                    .font(.largeTitle)
            }

            HStack {
                Text(poolGamblerBet.homeTeamName)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Text(poolGamblerBet.homeTeamMatchRawValue())
                    .font(.title)
                    .multilineTextAlignment(.center)
                    .scoreWidth()

                Text(poolGamblerBet.homeTeamBetRawValue())
                    .font(.caption)
                    .multilineTextAlignment(.center)
                    .scoreWidth()
            }

            HStack {
                Text(poolGamblerBet.awayTeamName)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Text(poolGamblerBet.awayTeamMatchRawValue())
                    .font(.title)
                    .multilineTextAlignment(.center)
                    .scoreWidth()

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
    FinishedBetItem(poolGamblerBet: poolGamblerBetDummyModel())
}
