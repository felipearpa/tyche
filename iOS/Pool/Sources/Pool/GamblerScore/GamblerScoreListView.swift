import SwiftUI
import Core
import UI
import DataPool

public struct GamblerScoreListView: View {
    @StateObject private var viewModel: GamblerScoreListViewModel
    private let onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)?
    @Environment(\.boxSpacing) private var boxSpacing

    public init(
        viewModel: @autoclosure @escaping () -> GamblerScoreListViewModel,
        onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)? = nil
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onGamblerOpen = onGamblerOpen
    }

    public var body: some View {
        GamblerScoreList(
            lazyPagingItems: viewModel.lazyPager,
            isCurrentUser: viewModel.gamblerId,
            onGamblerOpen: onGamblerOpen
        )
        // The top spacing stays outside the scroll view, keeping rows out from under the
        // navigation bar: its large title does not collapse on every pool-home tab.
        .padding(.top, boxSpacing.medium)
    }
}

#Preview {
    NavigationStack {
        GamblerScoreListView(
            viewModel: GamblerScoreListViewModel(
                getPoolGamblerScoresByPoolUseCase: GetPoolGamblerScoresByPoolUseCase(
                    poolGamblerScoreRepository: PoolGamblerScoreFakeRepository()
                ),
                gamblerId: "gambler-id",
                poolId: "pool-id"
            )
        )
    }
}
