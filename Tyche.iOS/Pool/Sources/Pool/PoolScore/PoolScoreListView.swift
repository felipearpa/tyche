import SwiftUI
import Core
import UI
import DataPool

private let defaultDebounceTimeInMilliseconds = 700

public struct PoolScoreListView: View {
    @StateObject private var viewModel: PoolScoreListViewModel
    @StateObject private var searchDebounceText = DebounceString(dueTime: .milliseconds(defaultDebounceTimeInMilliseconds))
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
        .padding(8)
        .searchable(
            text: $searchDebounceText.text,
            placement: .navigationBarDrawer(displayMode: .automatic),
            prompt: String(sharedResource: .searchingLabel)
        )
        .refreshable {
            viewModel.lazyPager.refresh()
        }
        .onAppearOnce {
            viewModel.lazyPager.refresh()
        }
        .onChange(of: searchDebounceText.debouncedText) { newSearchText in
            viewModel.search(newSearchText)
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
