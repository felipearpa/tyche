import SwiftUI
import UI
import Core

/// A pending match with the gambler's editable bet. Loading slots render this same component
/// from a placeholder model in visualization state with `isPlaceholder: true`: native redaction
/// under the shared `LoadingPlaceholderPulse` conceals its content, and it ignores touches and
/// stays out of the accessibility tree, so no bet field can be focused or edited.
struct PendingBetItem: View {
    let poolGamblerBet: PoolGamblerBetModel
    @Binding var viewState: PendingBetItemViewState
    let isPlaceholder: Bool
    @Namespace private var scoreNamespace

    @Environment(\.boxSpacing) private var boxSpacing

    init(
        poolGamblerBet: PoolGamblerBetModel,
        viewState: Binding<PendingBetItemViewState>,
        isPlaceholder: Bool = false
    ) {
        self.poolGamblerBet = poolGamblerBet
        self._viewState = viewState
        self.isPlaceholder = isPlaceholder
    }

    var body: some View {
        Group {
            if isPlaceholder {
                PulsingPlaceholderContent { content }
            } else {
                content
            }
        }
        .allowsHitTesting(!isPlaceholder)
        .accessibilityHidden(isPlaceholder)
    }

    private var content: some View {
        VStack(spacing: boxSpacing.medium) {
            teamRow(
                teamId: poolGamblerBet.homeTeamId,
                teamName: poolGamblerBet.homeTeamName,
                bet: $viewState.value.homeTeamBet,
                geoID: "home",
                autoFocus: true
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
        geoID: String,
        autoFocus: Bool = false
    ) -> some View {
        HStack {
            FlagImage(teamCode: teamId)
                .frame(width: flagSize, height: flagSize)
            Text(teamName)
                .frame(maxWidth: .infinity, alignment: .leading)
            scoreCell(bet: bet, geoID: geoID, autoFocus: autoFocus)
        }
    }

    @ViewBuilder
    private func scoreCell(bet: Binding<String>, geoID: String, autoFocus: Bool = false) -> some View {
        switch viewState {
        case .visualization:
            Text(bet.wrappedValue)
                .matchedGeometryEffect(id: geoID, in: scoreNamespace)
                .transition(.opacity)
        case .edition:
            BetTextField(value: bet, autoFocus: autoFocus)
                .font(.body)
                .scoreWidth()
                .matchedGeometryEffect(id: geoID, in: scoreNamespace)
                .transition(.opacity)
        }
    }
}

private let flagSize: CGFloat = 32

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
        viewState: .constant(.visualization(partialPoolGamblerBetFakeModel())),
        isPlaceholder: true
    )
}
