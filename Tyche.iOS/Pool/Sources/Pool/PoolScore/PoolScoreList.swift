import SwiftUI
import Core
import UI

struct PoolScoreList: View {
    let lazyPager: LazyPager<String, PoolGamblerScoreModel>
    let onPoolDetailRequested: (String) -> Void
    let onJoin: (String) -> Void

    var body: some View {
        let _ = Self._printChanges()
        
        PagingVStack(
            lazyPager: lazyPager,
            loadingContent: { PoolScoreFakeList() },
            loadingContentOnConcatenate: {
                PoolScoreItem(poolGamblerScore: fakePoolGamblerScoreModel(), onJoin: {})
                    .shimmer()
                Divider()
            }
        ) { poolGamblerScore in
            Group {
                PoolScoreItem(poolGamblerScore: poolGamblerScore, onJoin: { onJoin(poolGamblerScore.poolId) })
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
                    PoolScoreItem(poolGamblerScore: poolGamblerScore, onJoin: {})
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
        onPoolDetailRequested: { _ in },
        onJoin: { _ in }
    )
}

#Preview {
    PoolScoreFakeList()
}
