import SwiftUI
import UI

struct PoolScoreItem: View {
    let poolGamblerScore: PoolGamblerScoreModel
    let onJoin: () -> Void

    @State var optionsRevealed = false
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        HStack(spacing: boxSpacing.medium) {
            VStack(alignment:.leading) {
                Text(poolGamblerScore.poolName)
                if let currentPosition = poolGamblerScore.currentPosition {
                    Text(String(format: String(.positionLabel), String(currentPosition)))
                        .font(.footnote)
                }
            }

            Spacer()

            if let difference = poolGamblerScore.difference() {
                TrendIndicator(difference: difference)
            }
        }
        .frame(minHeight: minHeight)
        .swipeActions(edge: .trailing) {
            Button(action: onJoin) {
                Image(.personAdd)
            }
            .background(Color(sharedResource: .primaryContainer))
            .foregroundStyle(Color(sharedResource: .onPrimaryContainter))
        }
        .enableViewSwipeActions()
        .padding(boxSpacing.medium)
    }
}

private let minHeight: CGFloat = 68

#Preview {
    PoolScoreItem(poolGamblerScore: poolGamblerScoreDummyModel(), onJoin: {})
}

#Preview {
    PoolScoreItem(poolGamblerScore: poolGamblerScoreDummyModel(), onJoin: {})
        .shimmer()
}
