import SwiftUI

struct BetTimelineItem: View {
    let poolGamblerBet: PoolGamblerBetModel

    var body: some View {
        if poolGamblerBet.isComputed {
            FinishedBetItem(poolGamblerBet: poolGamblerBet)
        } else {
            NonEditablePendingBetItem(
                poolGamblerBet: poolGamblerBet,
                partialPoolGamblerBet: poolGamblerBet.toPartial()
            )
        }
    }
}

#Preview("Finished") {
    BetTimelineItem(poolGamblerBet: poolGamblerBetDummyModel())
}

#Preview("Live") {
    BetTimelineItem(
        poolGamblerBet: poolGamblerBetDummyModel().copy { $0.isComputed = false }
    )
}
