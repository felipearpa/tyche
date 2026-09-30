import SwiftUI
import Core
import UI
import DataPool

public struct GamblerScoreListView: View {
    @StateObject private var viewModel: GamblerScoreListViewModel
    private let onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)?

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
