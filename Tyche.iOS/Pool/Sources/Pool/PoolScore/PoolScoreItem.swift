import SwiftUI
import UI

struct PoolScoreItem: View {
    let poolGamblerScore: PoolGamblerScoreModel
    
    @Environment(\.boxSpacing) private var boxSpacing
    
    var body: some View {
        HStack {
            VStack(alignment:.leading) {
                Text(poolGamblerScore.poolName)
                if let currentPosition = poolGamblerScore.currentPosition {
                    Text(String(format: String(.positionLabel), String(currentPosition)))
                        .font(.footnote)
                }
            }
            
            Spacer()
            
            if let difference = poolGamblerScore.difference() {
                ProgressIndicator(difference: difference)
            }
        }
    }
}

#Preview {
    PoolScoreItem(poolGamblerScore: poolGamblerScoreModel())
}

#Preview {
    PoolScoreItem(poolGamblerScore: poolGamblerScoreModel())
        .shimmer()
}
