import SwiftUI
import Core
import UI

struct PoolScoreList: View {
    let lazyPager: LazyPager<String, PoolGamblerScoreModel>
    let onPoolDetailRequested: (String) -> Void
    
    var body: some View {
        let _ = Self._printChanges()
        
        PagingVStack(
            lazyPager: lazyPager,
            loadingContent: { PoolScoreFakeList() },
            loadingContentOnConcatenate: {
                PoolScoreItem(poolGamblerScore: fakePoolGamblerScoreModel())
                    .shimmer()
                Divider()
            }
        ) { poolGamblerScore in
            PoolScoreItem(poolGamblerScore: poolGamblerScore)
                .onTapGesture {
                    onPoolDetailRequested(poolGamblerScore.poolId)
                }
            Divider()
        }
    }
}

struct PoolScoreFakeList : View {
    private let poolGamblerScores: [PoolGamblerScoreModel] = (1...50).lazy.map { _ in
        fakePoolGamblerScoreModel()
    }
    
    var body: some View {
        LazyVStack(spacing: 8) {
            ForEach(poolGamblerScores) { poolGamblerScore in
                PoolScoreItem(poolGamblerScore: poolGamblerScore)
                    .shimmer()
                Divider()
            }
        }
    }
}

struct PoolScoreList_Previews: PreviewProvider {
    static var previews: some View {
        PoolScoreList(
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
            onPoolDetailRequested: { _ in }
        )
    }
}

struct PoolScoreFakeList_Previews: PreviewProvider {
    static var previews: some View {
        PoolScoreFakeList()
    }
}
