import SwiftUI

struct PoolGamblerBetFakeItem: View {
    var body: some View {
        PoolGamblerBetItem(
            poolGamblerBet: poolGamblerBetFakeModel(),
            viewState: .constant(.visualization(partialPoolGamblerBetFakeModel()))
        ).shimmer()
    }
}

#Preview {
    PoolGamblerBetFakeItem()
}
