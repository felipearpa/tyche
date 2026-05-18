import SwiftUI
import UI
import Core

struct PendingBetItem: View {
    let poolGamblerBet: PoolGamblerBetModel
    @Binding var viewState: PendingBetItemViewState
    @Namespace private var scoreNamespace

    @Environment(\.boxSpacing) private var boxSpacing

    init(
        poolGamblerBet: PoolGamblerBetModel,
        viewState: Binding<PendingBetItemViewState>
    ) {
        self.poolGamblerBet = poolGamblerBet
        self._viewState = viewState
    }

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
            teamRow(
                teamId: poolGamblerBet.homeTeamId,
                teamName: poolGamblerBet.homeTeamName,
                bet: $viewState.value.homeTeamBet,
                geoID: "home"
            )

            teamRow(
                teamId: poolGamblerBet.awayTeamId,
                teamName: poolGamblerBet.awayTeamName,
                bet: $viewState.value.awayTeamBet,
                geoID: "away"
            )

            if case .visualization = viewState {
                HStack {
                    Text(poolGamblerBet.matchDateTime.toShortDateTimeString())
                        .font(.footnote)
                        .padding(.leading, boxSpacing.medium)
                    Spacer()
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
    }

    @ViewBuilder
    private func teamRow(
        teamId: String,
        teamName: String,
        bet: Binding<String>,
        geoID: String
    ) -> some View {
        HStack {
            FlagImage(teamCode: teamId)
                .frame(width: flagSize, height: flagSize)
            Text(teamName)
                .frame(maxWidth: .infinity, alignment: .leading)
            scoreCell(bet: bet, geoID: geoID)
        }
    }

    @ViewBuilder
    private func scoreCell(bet: Binding<String>, geoID: String) -> some View {
        switch viewState {
        case .visualization:
            Text(bet.wrappedValue)
                .matchedGeometryEffect(id: geoID, in: scoreNamespace)
                .transition(.opacity)
        case .edition:
            BetTextField(value: bet)
                .betStyle()
                .matchedGeometryEffect(id: geoID, in: scoreNamespace)
                .transition(.opacity)
        }
    }
}

private let flagSize: CGFloat = 32

private extension View {
    func betStyle() -> some View {
        let textWidth = String(repeating: "X", count: 4)
            .widthOfString(usingFont: UIFont.preferredFont(from: .body))
        return self.frame(width: textWidth)
            .font(.body)
    }
}

#Preview("Non Editable") {
    PendingBetItem(
        poolGamblerBet: poolGamblerBetDummyModel(),
        viewState: .constant(.visualization(partialPoolGamblerBetDummyModel()))
    )
}

#Preview("Editable") {
    PendingBetItem(
        poolGamblerBet: poolGamblerBetDummyModel(),
        viewState: .constant(.edition(partialPoolGamblerBetDummyModel()))
    )
}

#Preview("Placeholder") {
    PendingBetItem(
        poolGamblerBet: poolGamblerBetPlaceholderModel(isLocked: false, isComputed: false),
        viewState: .constant(.visualization(PartialPoolGamblerBetModel(homeTeamBet: "", awayTeamBet: "")))
    )
    .shimmer()
}
