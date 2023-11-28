import SwiftUI
import UI
import Core

struct PoolGamblerBetItem: View {
    private let poolGamblerBet: PoolGamblerBetModel
    @Binding private var viewState: PoolGamblerBetItemViewState
    
    init(
        poolGamblerBet: PoolGamblerBetModel,
        viewState: Binding<PoolGamblerBetItemViewState>
    ) {
        self.poolGamblerBet = poolGamblerBet
        self._viewState = viewState
    }
    
    var body: some View {
        switch viewState {
        case .visualization:
            NonEditablePoolGamblerBetItem(
                poolGamblerBet: poolGamblerBet,
                partialPoolGamblerBet: viewState.value
            )
        case .edition:
            EditablePoolGamblerBetItem(
                poolGamblerBet: poolGamblerBet,
                partialPoolGamblerBet: $viewState.value
            )
        }
    }
}

private struct NonEditablePoolGamblerBetItem: View {
    let poolGamblerBet: PoolGamblerBetModel
    let partialPoolGamblerBet: PartialPoolGamblerBetModel
    
    var body: some View {
        VStack(spacing: 8) {
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
                    .padding(.leading, 8)
                Spacer()
            }
        }
    }
}

private struct EditablePoolGamblerBetItem: View {
    let poolGamblerBet: PoolGamblerBetModel
    @Binding var partialPoolGamblerBet: PartialPoolGamblerBetModel
    
    var body: some View {
        VStack(spacing: 8) {
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

#Preview("Non Editable PoolGamblerBetItem") {
    NonEditablePoolGamblerBetItem(
        poolGamblerBet: poolGamblerBetDummyModel(),
        partialPoolGamblerBet: partialPoolGamblerBetDummyModel()
    )
}

#Preview("Editable PoolGamblerBetItem") {
    EditablePoolGamblerBetItem(
        poolGamblerBet: poolGamblerBetDummyModel(),
        partialPoolGamblerBet: .constant(partialPoolGamblerBetDummyModel())
    )
}
