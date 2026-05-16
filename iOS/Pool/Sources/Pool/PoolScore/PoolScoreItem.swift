import SwiftUI
import UI

struct PoolScoreItem: View {
    let poolGamblerScore: PoolGamblerScoreModel
    let onJoin: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    private let inviteIconSize: CGFloat = 20

    var body: some View {
        HStack(spacing: boxSpacing.medium) {
            if let position = poolGamblerScore.position {
                PostionIndicator(position: position, shouldUsePrimeryColor: false)
            }

            Text(poolGamblerScore.poolName)

            Spacer()

            if let difference = poolGamblerScore.difference() {
                TrendIndicator(difference: difference)
            }

            Button(action: onJoin) {
                Image(.personAdd)
                    .resizable()
                    .scaledToFit()
                    .frame(width: inviteIconSize, height: inviteIconSize)
            }
            .buttonStyle(.liquidGlass)
            .buttonBorderShape(.capsule)
        }
        .padding(boxSpacing.large)
        .frame(maxWidth: .infinity)
    }
}

#Preview("Normal Light") {
    PoolScoreItem(poolGamblerScore: poolGamblerScoreDummyModel(), onJoin: {})
        .preferredColorScheme(.light)
}

#Preview("Normal Dark") {
    PoolScoreItem(poolGamblerScore: poolGamblerScoreDummyModel(), onJoin: {})
        .preferredColorScheme(.dark)
}

#Preview("Without position") {
    PoolScoreItem(poolGamblerScore: poolGamblerScoreWithoutPositionDummyModel(), onJoin: {})
}

#Preview("Placeholder") {
    PoolScoreItem(poolGamblerScore: poolGamblerScoreDummyModel(), onJoin: {})
        .shimmer()
}
