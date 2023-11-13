import SwiftUI
import Core
import UI

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

struct PoolScoreListView_Previews: PreviewProvider {
    private class PoolGamblerScoreFakeRepository: PoolGamblerScoreRepository {
        func getPoolGamblerScoresByGambler(
            gamblerId: String,
            next: String?,
            searchText: String?
        ) async -> Result<CursorPage<PoolGamblerScore>, Error> {
            .success(
                CursorPage(
                    items: poolGamblerScores(),
                    next: nil
                )
            )
        }
        
        func getPoolGamblerScoresByPool(
            poolId: String,
            next: String?,
            searchText: String?
        ) async -> Result<Core.CursorPage<PoolGamblerScore>, Error> {
            .success(
                CursorPage(
                    items: poolGamblerScores(),
                    next: nil
                )
            )
        }
    }
    
    static var previews: some View {
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
}
