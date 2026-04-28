import SwiftUI
import UI

struct GamblerScoreItem: View {
    let poolGamblerScore: PoolGamblerScoreModel
    let isCurrentUser: Bool
    let onTap: ((_ poolId: String, _ gamblerId: String) -> Void)?

    @Environment(\.boxSpacing) private var boxSpacing

    init(
        poolGamblerScore: PoolGamblerScoreModel,
        isCurrentUser: Bool,
        onTap: ((_ poolId: String, _ gamblerId: String) -> Void)? = nil
    ) {
        self.poolGamblerScore = poolGamblerScore
        self.isCurrentUser = isCurrentUser
        self.onTap = onTap
    }

    var body: some View {
        let row = rowContent
        if let onTap {
            Button(action: { onTap(poolGamblerScore.poolId, poolGamblerScore.gamblerId) }) {
                row
            }
            .buttonStyle(.plain)
        } else {
            row
        }
    }

    private var rowContent: some View {
        HStack(spacing: boxSpacing.medium) {
            if let currentPosition = poolGamblerScore.position {
                ZStack {
                    Text(String(currentPosition))
                }
                .frame(width: indicatorSize, height: indicatorSize)
                .background(isCurrentUser ? Color(sharedResource: .primaryContainer) : Color(sharedResource: .secondaryContainer))
                .foregroundColor(isCurrentUser ? Color(sharedResource: .onPrimaryContainter) : Color(sharedResource: .onSecondaryContainer))
                .clipShape(Circle())
            } else {
                Color.clear.frame(width: indicatorSize, height: indicatorSize)
            }

            Text(poolGamblerScore.gamblerUsername).fontWeight(isCurrentUser ? .bold : .regular)

            Spacer()

            if let score = poolGamblerScore.score {
                Text(String(score))
            }

            if let difference = poolGamblerScore.difference() {
                TrendIndicator(difference: difference)
                    .frame(width: indicatorSize)
            } else {
                Color.clear.frame(width: indicatorSize)
            }
        }
    }
}

private let indicatorSize = 32.0

#Preview("Default") {
    GamblerScoreItem(poolGamblerScore: poolGamblerScoreDummyModel(), isCurrentUser: true)
}

#Preview("Not position Data") {
    GamblerScoreItem(
        poolGamblerScore: poolGamblerScoreDummyModelWithoutPositionData(),
        isCurrentUser: true
    )
}
