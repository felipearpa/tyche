import SwiftUI
import Core
import UI

struct PoolScoreList: View {
    let lazyPager: LazyPager<String, PoolGamblerScoreModel>
    let onPoolDetailRequested: (String) -> Void
    
    @Environment(\.boxSpacing) private var boxSpacing
    
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
            Group {
                PoolScoreItem(poolGamblerScore: poolGamblerScore)
                    .padding([.horizontal], boxSpacing.medium)
                    .padding([.vertical], boxSpacing.small)
                Divider()
            }.onTapGesture {
                onPoolDetailRequested(poolGamblerScore.poolId)
            }
        }
    }
}

struct PoolScoreFakeList : View {
    @Environment(\.boxSpacing) private var boxSpacing
    
    private let poolGamblerScores: [PoolGamblerScoreModel] = (1...50).lazy.map { _ in
        fakePoolGamblerScoreModel()
    }
    
    var body: some View {
        ScrollView {
            LazyVStack(spacing: boxSpacing.medium) {
                ForEach(poolGamblerScores) { poolGamblerScore in
                    PoolScoreItem(poolGamblerScore: poolGamblerScore)
                        .shimmer()
                    Divider()
                }
            }
        }
    }
}

#Preview {
    PoolScoreList(
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
        onPoolDetailRequested: { _ in }
    )
}

#Preview {
    PoolScoreFakeList()
}
