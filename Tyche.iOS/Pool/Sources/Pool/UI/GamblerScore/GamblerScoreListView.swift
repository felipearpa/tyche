import SwiftUI
import Core
import UI

public struct GamblerScoreListView: View {
    @StateObject private var viewModel: GamblerScoreListViewModel
    
    public init(viewModel: @autoclosure @escaping () -> GamblerScoreListViewModel) {
        self._viewModel = .init(wrappedValue: viewModel())
    }
    
    public var body: some View {
        let _ = Self._printChanges()
        
        GamblerScoreList(
            lazyPager: viewModel.lazyPager,
            loggedInGamblerId: viewModel.gamblerId
        )
        .padding(8)
        .refreshable {
            viewModel.lazyPager.refresh()
        }
        .onAppearOnce {
            viewModel.lazyPager.refresh()
        }
    }
}

struct GamblerScoreListView_Previews: PreviewProvider {
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
        ) async -> Result<CursorPage<PoolGamblerScore>, Error> {
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
}
