import SwiftUI
import UI

struct FinishedPoolGamblerBetItem: View {
    let poolGamblerBet: PoolGamblerBetModel

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
            HStack {
                Text(poolGamblerBet.homeTeamName)
                    .frame(maxWidth: .infinity, alignment: .leading)
                
                HStack(spacing: boxSpacing.medium) {
                    Text(poolGamblerBet.homeTeamMatchRawValue())
                        .multilineTextAlignment(.center)
                        .scoreWidth()
                    
                    Text(poolGamblerBet.homeTeamBetRawValue())
                        .multilineTextAlignment(.center)
                        .betScoreStyle()
                }
            }
            
            HStack {
                Text(poolGamblerBet.awayTeamName)
                    .frame(maxWidth: .infinity, alignment: .leading)
                
                HStack(spacing: boxSpacing.medium) {
                    Text(poolGamblerBet.awayTeamMatchRawValue())
                        .multilineTextAlignment(.center)
                        .scoreWidth()
                    
                    Text(poolGamblerBet.awayTeamBetRawValue())
                        .multilineTextAlignment(.center)
                        .betScoreStyle()
                }
            }
            
            HStack {
                Text(poolGamblerBet.matchDateTime.toShortDateTimeString())
                    .font(.footnote)
                    .padding(.leading, boxSpacing.large)

                Spacer()
                
                Text(poolGamblerBet.score.map { String($0) } ?? "")
                    .font(.title3)
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

    func betScoreStyle() -> some View {
        let height: CGFloat = UIFont.preferredFont(from: .body).lineHeight
        return self.scoreWidth()
            .background(
                RoundedRectangle(cornerRadius: height / 2)
                    .fill(Color(sharedResource: .secondaryContainer))
            )
            .foregroundColor(Color(sharedResource: .onSecondaryContainer))
    }
}

#Preview {
    FinishedPoolGamblerBetItem(poolGamblerBet: poolGamblerBetDummyModel())
}
