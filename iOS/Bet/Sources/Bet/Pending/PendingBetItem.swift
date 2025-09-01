import SwiftUI
import UI
import Core

struct PendingBetItem: View {
    private let poolGamblerBet: PoolGamblerBetModel
    @Binding private var viewState: PendingBetItemViewState
    
    init(
        poolGamblerBet: PoolGamblerBetModel,
        viewState: Binding<PendingBetItemViewState>
    ) {
        self.poolGamblerBet = poolGamblerBet
        self._viewState = viewState
    }
    
    var body: some View {
        switch viewState {
        case .visualization:
            NonEditablePendingBetItem(
                poolGamblerBet: poolGamblerBet,
                partialPoolGamblerBet: viewState.value
            )
        case .edition:
            EditablePendingBetItem(
                poolGamblerBet: poolGamblerBet,
                partialPoolGamblerBet: $viewState.value
            )
        }
    }
}

private struct NonEditablePendingBetItem: View {
    let poolGamblerBet: PoolGamblerBetModel
    let partialPoolGamblerBet: PartialPoolGamblerBetModel

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
            HStack {
                Text(poolGamblerBet.homeTeamName)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(partialPoolGamblerBet.homeTeamBet)
            }
            
            HStack {
                Text(poolGamblerBet.awayTeamName)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(partialPoolGamblerBet.awayTeamBet)
            }
            
            HStack {
                Text(poolGamblerBet.matchDateTime.toShortDateTimeString())
                    .font(.footnote)
                    .padding(.leading, boxSpacing.medium)
                Spacer()
            }
        }
    }
}

private struct EditablePendingBetItem: View {
    let poolGamblerBet: PoolGamblerBetModel
    @Binding var partialPoolGamblerBet: PartialPoolGamblerBetModel

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
            HStack {
                Text(poolGamblerBet.homeTeamName)
                    .frame(maxWidth: .infinity, alignment: .leading)
                BetTextField(value: $partialPoolGamblerBet.homeTeamBet)
                    .betStyle()
            }
            
            HStack {
                Text(poolGamblerBet.awayTeamName)
                    .frame(maxWidth: .infinity, alignment: .leading)
                BetTextField(value: $partialPoolGamblerBet.awayTeamBet)
                    .betStyle()
            }
        }
    }
}

private extension View {
    func betStyle() -> some View {
        let textWidth = String(repeating: "X", count: 4)
            .widthOfString(usingFont: UIFont.preferredFont(from: .body))
        return self.frame(width: textWidth)
            .font(.body)
    }
}

#Preview("Non Editable") {
    NonEditablePendingBetItem(
        poolGamblerBet: poolGamblerBetDummyModel(),
        partialPoolGamblerBet: partialPoolGamblerBetDummyModel()
    )
}

#Preview("Editable") {
    EditablePendingBetItem(
        poolGamblerBet: poolGamblerBetDummyModel(),
        partialPoolGamblerBet: .constant(partialPoolGamblerBetDummyModel())
    )
}
