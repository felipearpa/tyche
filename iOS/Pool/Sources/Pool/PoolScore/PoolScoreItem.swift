import SwiftUI
import UI

struct PoolScoreItem: View {
    let poolGamblerScore: PoolGamblerScoreModel
    let onJoin: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    private let inviteIconSize: CGFloat = 20

    var body: some View {
        HStack(spacing: boxSpacing.medium) {
            VStack(spacing: boxSpacing.small) {
                if let position = poolGamblerScore.position {
                    PostionIndicator(position: position, shouldUsePrimeryColor: false)
                }

                if let rank = poolGamblerScore.rank() {
                    TrendIndicator(difference: rank, textStyle: Font.TextStyle.footnote)
                }
            }

            VStack(alignment: .leading) {
                Text(poolGamblerScore.poolName)

                HStack(spacing: boxSpacing.large) {
                    if let score = poolGamblerScore.score {
                        Text(.pointsText(score))
                            .font(.footnote.bold())
                    }

                    if let gamblerCount = poolGamblerScore.gamblerCount {
                        Text(.gamblersText(gamblerCount))
                            .font(.footnote)
                    }
                }
            }

            Spacer()

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

#Preview("Long pool name") {
    PoolScoreItem(
        poolGamblerScore: PoolGamblerScoreModel(
            poolId: "A3C2E1",
            poolName: "This is a very long pool name to test how it is displayed in the list",
            gamblerId: "YF23H1",
            gamblerUsername: "neptune-player",
            position: 4,
            beforePosition: 3,
            score: 8,
            gamblerCount: 101
        ),
        onJoin: {}
    )
}

#Preview("Placeholder") {
    PoolScoreItem(poolGamblerScore: poolGamblerScoreDummyModel(), onJoin: {})
        .shimmer()
}
