import SwiftUI
import UI
import Core

struct PoolGamblerBetItem: View {
    private var poolGamblerBet: PoolGamblerBetModel
    @Binding private var homeTeamBet: String
    @Binding private var awayTeamBet: String
    private var isEditable: Bool = false
    
    init(
        poolGamblerBet: PoolGamblerBetModel,
        homeTeamBet: Binding<String> = .constant(""),
        awayTeamBet: Binding<String> = .constant(""),
        isEditable: Bool = false
    ) {
        self.poolGamblerBet = poolGamblerBet
        self._homeTeamBet = homeTeamBet
        self._awayTeamBet = awayTeamBet
        self.isEditable = isEditable
    }
    
    var body: some View {
        let _ = Self._printChanges()
        
        VStack(spacing: isEditable ? 0 : 8) {
            HStack {
                Text(poolGamblerBet.homeTeamName)
                
                Spacer()

                if poolGamblerBet.isLocked || !isEditable {
                    Text(homeTeamBet)
                } else {
                    BetTextField(value: $homeTeamBet)
                        .betStyle()
                }
            }
            
            HStack {
                Text(poolGamblerBet.awayTeamName)
                
                Spacer()

                if poolGamblerBet.isLocked || !isEditable {
                    Text(awayTeamBet)
                } else {
                    BetTextField(value: $awayTeamBet)
                        .betStyle()
                }
            }
            
            HStack {
                Text(poolGamblerBet.matchDateTime.toShortDateTimeString())
                    .font(.footnote)
                
                Spacer()
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

//#Preview {
//    PoolGamblerBetItem(poolGamblerBet: poolGamblerBetModel(), onBet: { _, _ in })
//}
