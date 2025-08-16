import SwiftUI

public struct PoolSpotlightItem: View {
    let poolGamblerScore: PoolGamblerScoreModel

    public init(poolGamblerScore: PoolGamblerScoreModel) {
        self.poolGamblerScore = poolGamblerScore
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(poolGamblerScore.poolName)
                .font(.title)

            Text(poolGamblerScore.gamblerUsername)
                .font(.title3)
        }
    }
}

#Preview {
    PoolSpotlightItem(poolGamblerScore: poolGamblerScoreDummyModel())
}
