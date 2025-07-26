import SwiftUI
import Core
import UI

struct PoolScoreList: View {
    let lazyPager: LazyPager<String, PoolGamblerScoreModel>
    let onPoolOpen: (String) -> Void
    let onPoolJoin: (String) -> Void

    var body: some View {
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
                PoolScoreItem(poolGamblerScore: poolGamblerScore, onJoin: { onPoolJoin(poolGamblerScore.poolId) })
                Divider()
            }.onTapGesture {
                onPoolOpen(poolGamblerScore.poolId)
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
    let lazyPager = LazyPager(
        pagingData: PagingData(
            pagingConfig: PagingConfig(prefetchDistance: 5),
            pagingSourceFactory: PoolGamblerScorePagingSource(
                pagingQuery: { _ in .
                    success(CursorPage(items: poolGamblerScoresDummyModels(), next: nil))
                }
            )
        )
    )

    PoolScoreList(
        lazyPager: lazyPager,
        onPoolOpen: { _ in },
        onPoolJoin: { _ in }
    )
    .onAppearOnce {
        lazyPager.refresh()
    }
}

#Preview {
    PoolScoreFakeList()
}
