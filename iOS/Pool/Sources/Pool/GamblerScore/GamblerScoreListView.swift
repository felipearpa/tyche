import SwiftUI
import Core
import UI
import DataPool

public struct GamblerScoreListView: View {
    @StateObject private var viewModel: GamblerScoreListViewModel
    
    public init(viewModel: @autoclosure @escaping () -> GamblerScoreListViewModel) {
        self._viewModel = .init(wrappedValue: viewModel())
    }
    
    public var body: some View {
        GamblerScoreList(
            lazyPagingItems: viewModel.lazyPager,
            isCurrentUser: viewModel.gamblerId
        )
        .refreshable {
            await viewModel.lazyPager.refresh()
        }
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
