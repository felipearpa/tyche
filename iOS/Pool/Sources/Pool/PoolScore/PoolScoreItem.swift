import SwiftUI
import UI

struct PoolScoreItem: View {
    let poolGamblerScore: PoolGamblerScoreModel
    let onJoin: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
            HStack(spacing: boxSpacing.medium) {
                VStack(alignment: .leading, spacing: boxSpacing.small) {
                    Text(poolGamblerScore.poolName)
                    if let currentPosition = poolGamblerScore.position {
                        Text(String(format: String(.positionLabel), String(currentPosition)))
                            .font(.footnote)
                    }
                }

                Spacer()

                if let difference = poolGamblerScore.difference() {
                    TrendIndicator(difference: difference)
                }
            }
            .padding(boxSpacing.medium)

            HStack {
                Spacer()
                Button(action: onJoin) {
                    HStack(spacing: boxSpacing.medium) {
                        Image(.personAdd)
                        Text(String(.inviteAction))
                    }
                }
                .buttonStyle(.bordered)
            }
        }
        .padding(boxSpacing.medium)
        .frame(maxWidth: .infinity)
    }
}

#Preview {
    PoolScoreItem(poolGamblerScore: poolGamblerScoreDummyModel(), onJoin: {})
        .padding()
}

#Preview {
    PoolScoreItem(poolGamblerScore: poolGamblerScoreDummyModel(), onJoin: {})
        .shimmer()
        .padding()
}
