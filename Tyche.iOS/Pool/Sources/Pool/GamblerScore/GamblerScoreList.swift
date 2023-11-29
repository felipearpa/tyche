import SwiftUI
import Core
import UI

struct GamblerScoreList: View {
    var lazyPager: LazyPager<String, PoolGamblerScoreModel>
    let loggedInGamblerId: String?
    
    var body: some View {
        let _ = Self._printChanges()
        
        PagingVStack(
            lazyPager: lazyPager,
            loadingContent: { GamblerScoreFakeList() },
            loadingContentOnConcatenate: {
                PoolScoreItem(poolGamblerScore: fakePoolGamblerScoreModel())
                    .shimmer()
                Divider()
            }
        ) { poolGamblerScore in
            GamblerScoreItem(
                poolGamblerScore: poolGamblerScore,
                isLoggedIn: loggedInGamblerId != nil ? loggedInGamblerId == poolGamblerScore.gamblerId : false
            )
            
            Divider()
        }
    }
}

struct GamblerScoreFakeList : View {
    private let poolGamblerScores: [PoolGamblerScoreModel] = (1...50).lazy.map { _ in
        fakePoolGamblerScoreModel()
    }
    
    var body: some View {
        LazyVStack(spacing: 8) {
            ForEach(poolGamblerScores) { poolGamblerScore in
                GamblerScoreItem(
                    poolGamblerScore: poolGamblerScore,
                    isLoggedIn: false
                )
                .shimmer()
                
                Divider()
            }
        }
    }
}

#Preview {
    GamblerScoreList(
        lazyPager: LazyPager(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: PoolGamblerScorePagingSource(
                    pagingQuery: { _ in .
                        success(CursorPage(items: poolGamblerScoresDummyModels(), next: nil))
                    }
                )
            )
        ),
        loggedInGamblerId: "logged-in-gambler-id"
    )
}
