import SwiftUI
import UI

struct PoolScoreItem: View {
    let poolGamblerScore: PoolGamblerScoreModel
    let onJoin: () -> Void

    @State var optionsRevealed = false
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
        .frame(minHeight: minHeight)
        .swipeActions(edge: .trailing) {
            Button(action: onJoin) {
                Image(.personAdd)
            }
            .background(Color(sharedResource: .primaryContainer))
            .foregroundStyle(Color(sharedResource: .onPrimaryContainter))
        }
        .enableViewSwipeActions()
    }
}

private let minHeight: CGFloat = 68

#Preview {
    PoolScoreItem(poolGamblerScore: poolGamblerScoreModel(), onJoin: {})
}

#Preview {
    PoolScoreItem(poolGamblerScore: poolGamblerScoreModel(), onJoin: {})
        .shimmer()
}
