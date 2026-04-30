import SwiftUI
import UI

struct PoolScoreItem: View {
    let poolGamblerScore: PoolGamblerScoreModel
    let onJoin: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
            HStack(spacing: boxSpacing.medium) {
                if let position = poolGamblerScore.position {
                    PostionIndicator(postion: position, isSignedIdUser: false)
                }

                Text(poolGamblerScore.poolName)

                Spacer()

                if let difference = poolGamblerScore.difference() {
                    TrendIndicator(difference: difference)
                }

                Spacer().frame(width: boxSpacing.medium)

                Button { onJoin() } label: {
                    Image(.personAdd)
                }
                .labelStyle(.iconOnly)
            }
        }
        .padding(boxSpacing.medium)
        .frame(maxWidth: .infinity)
    }
}

#Preview("Normal") {
    PoolScoreItem(poolGamblerScore: poolGamblerScoreDummyModel(), onJoin: {})
        .padding()
}

#Preview("Placeholder") {
    PoolScoreItem(poolGamblerScore: poolGamblerScoreDummyModel(), onJoin: {})
        .shimmer()
        .padding()
}
