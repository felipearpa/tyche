import SwiftUI

/// A history row: the finished or live presentation for the bet. `isPlaceholder` passes through
/// to the chosen item, which owns the placeholder rendering, so the pulse is applied once.
struct BetTimelineItem: View {
    let poolGamblerBet: PoolGamblerBetModel
    let isPlaceholder: Bool

    init(poolGamblerBet: PoolGamblerBetModel, isPlaceholder: Bool = false) {
        self.poolGamblerBet = poolGamblerBet
        self.isPlaceholder = isPlaceholder
    }

    var body: some View {
        if poolGamblerBet.isComputed {
            FinishedBetItem(poolGamblerBet: poolGamblerBet, isPlaceholder: isPlaceholder)
        } else {
            LiveBetItem(poolGamblerBet: poolGamblerBet, isPlaceholder: isPlaceholder)
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
