import SwiftUI
import UI

struct PoolScoreItem: View {
    let poolGamblerScore: PoolGamblerScoreModel
    
    var body: some View {
        HStack {
            VStack(alignment:.leading) {
                Text(poolGamblerScore.poolName)
                if let currentPosition = poolGamblerScore.currentPosition {
                    Text(String(format: StringScheme.positionLabel.localizedString, String(currentPosition)))
                        .font(.footnote)
                }
            }
            
            Spacer()
            
            if let difference = poolGamblerScore.calculateDifference() {
                ProgressIndicator(difference: difference)
            }
        }
    }
}

struct PoolScoreItem_Previews: PreviewProvider {
    static var previews: some View {
        PoolScoreItem(poolGamblerScore: poolGamblerScoreModel())
    }
}

struct PoolScoreFakeItem_Previews: PreviewProvider {
    static var previews: some View {
        PoolScoreItem(poolGamblerScore: poolGamblerScoreModel())
            .shimmer()
    }
}