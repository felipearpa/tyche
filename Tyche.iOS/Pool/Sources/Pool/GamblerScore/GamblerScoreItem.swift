import SwiftUI
import UI

struct GamblerScoreItem: View {
    let poolGamblerScore: PoolGamblerScoreModel
    let isCurrentUser: Bool

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        HStack(spacing: boxSpacing.medium) {
            if let currentPosition = poolGamblerScore.currentPosition {
                ZStack {
                    Text(String(currentPosition))
                }
                .frame(width: indicatorSize, height: indicatorSize)
                .background(isCurrentUser ? Color(sharedResource: .primaryContainer) : Color(sharedResource: .secondaryContainer))
                .foregroundColor(isCurrentUser ? Color(sharedResource: .onPrimaryContainter) : Color(sharedResource: .onSecondaryContainer))
                .clipShape(Circle())
            } else {
                Color.clear.frame(width: indicatorSize)
            }
            
            Text(poolGamblerScore.gamblerUsername)
            
            Spacer()
            
            if let score = poolGamblerScore.score {
                Text(String(score))
            }
            
            if let difference = poolGamblerScore.difference() {
                TrendIndicator(difference: difference)
                    .frame(width: indicatorSize)
            } else {
                Color.clear.frame(width: indicatorSize)
            }
        }
    }
}

private let indicatorSize = 32.0

#Preview("Default") {
    GamblerScoreItem(poolGamblerScore: poolGamblerScoreDummyModel(), isCurrentUser: true)
}

#Preview("Not position Data") {
    GamblerScoreItem(
        poolGamblerScore: poolGamblerScoreDummyModelWithoutPositionData(),
        isCurrentUser: true
    )
}
