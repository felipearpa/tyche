import SwiftUI
import Core
import UI
import DataPool

public struct PoolScoreListView: View {
    @StateObject private var viewModel: PoolScoreListViewModel
    private let onPoolDetailRequested: (PoolProfile) -> Void
    
    public init(
        viewModel: @autoclosure @escaping () -> PoolScoreListViewModel,
        onPoolDetailRequested: @escaping (PoolProfile) -> Void)
    {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onPoolDetailRequested = onPoolDetailRequested
    }
    
    public var body: some View {
        let _ = Self._printChanges()
        
        PoolScoreList(
            lazyPager: viewModel.lazyPager,
            onPoolDetailRequested: { poolId in
                onPoolDetailRequested(PoolProfile(poolId: poolId))
            }
        )
        .navigationTitle(String(.gamblerPoolListTitle))
        .refreshable {
            viewModel.lazyPager.refresh()
        }
        .onAppearOnce {
            viewModel.lazyPager.refresh()
        }
    }
}

#Preview {
    NavigationStack {
        PoolScoreListView(
            viewModel: PoolScoreListViewModel(
                getPoolGamblerScoresByGamblerUseCase: GetPoolGamblerScoresByGamblerUseCase(
                    poolGamblerScoreRepository: PoolGamblerScoreFakeRepository()
                ),
                gamblerId: "gambler-id"
            ),
            onPoolDetailRequested: { _ in }
        )
    }
}
