import SwiftUI
import Core
import UI

struct GamblerScoreList: View {
    @ObservedObject var lazyPager: LazyPager<String, PoolGamblerScoreModel>
    let loggedInGamblerId: String?
    
    var body: some View {
        PagingVStack(
            lazyPager: lazyPager,
            loadingContent: { PoolScoreFakeList() },
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
        ScrollView {
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
}

struct GamblerScoreList_Previews: PreviewProvider {
    static var previews: some View {
        GamblerScoreList(
            lazyPager: LazyPager(
                pagingData: PagingData(
                    pagingConfig: PagingConfig(prefetchDistance: 5),
                    pagingSourceFactory: PoolGamblerScorePagingSource(
                        pagingQuery: { _ in .
                            success(CursorPage(items: poolGamblerScoreModels(), next: nil))
                        }
                    )
                )
            ),
            loggedInGamblerId: "logged-in-gambler-id"
        )
    }
}

struct GamblerScoreFakeList_Previews: PreviewProvider {
    static var previews: some View {
        GamblerScoreFakeList()
    }
}
