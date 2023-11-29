import SwiftUI
import UI

private let indicatorSize = 32.0

struct GamblerScoreItem: View {
    let poolGamblerScore: PoolGamblerScoreModel
    let isLoggedIn: Bool
    
    var body: some View {
        HStack(spacing: 8) {
            if let currentPosition = poolGamblerScore.currentPosition {
                ZStack {
                    Text(String(currentPosition))
                }
                .frame(width: indicatorSize, height: indicatorSize)
                .background(isLoggedIn ? Color(sharedResource: .primaryContainer) : Color(sharedResource: .secondaryContainer))
                .foregroundColor(isLoggedIn ? Color(sharedResource: .onPrimaryContainter) : Color(sharedResource: .onSecondaryContainer))
                .clipShape(Circle())
            } else {
                Text("")
                    .frame(width: indicatorSize)
            }
            
            Text(poolGamblerScore.gamblerUsername)
            
            Spacer()
            
            if let score = poolGamblerScore.score {
                Text(String(score))
            }
            
            if let difference = poolGamblerScore.difference() {
                ProgressIndicator(difference: difference)
                    .frame(width: indicatorSize)
            } else {
                Text("")
                    .frame(width: indicatorSize)
            }
        }
    }
}

struct GamblerScoreItem_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            GamblerScoreItem(poolGamblerScore: poolGamblerScoreModel(), isLoggedIn: true)
                .previewLayout(.sizeThatFits)
                .previewDisplayName("Default")
            
            GamblerScoreItem(
                poolGamblerScore: poolGamblerScoreModelWithoutPositionData(),
                isLoggedIn: true
            )
            .previewLayout(.sizeThatFits)
            .previewDisplayName("Not position Data")
        }
    }
}
