import SwiftUI
import Core
import UI

struct GamblerScoreList: View {
    var lazyPager: LazyPager<String, PoolGamblerScoreModel>
    let isCurrentUser: String?

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        let _ = Self._printChanges()

        PagingVStack(
            lazyPager: lazyPager,
            loadingContent: { GamblerScoreFakeList() },
            loadingContentOnConcatenate: {
                PoolScoreItem(poolGamblerScore: poolGamblerScorePlaceholderModel(), onJoin: {})
                    .shimmer()
                Divider()
            }
        ) { poolGamblerScore in
            GamblerScoreItem(
                poolGamblerScore: poolGamblerScore,
                isCurrentUser: isCurrentUser != nil ? isCurrentUser == poolGamblerScore.gamblerId : false
            )
            .padding([.horizontal], boxSpacing.medium)
            .padding([.vertical], boxSpacing.small)

            Divider()
        }
    }
}

struct GamblerScoreFakeList : View {
    @Environment(\.boxSpacing) private var boxSpacing

    private let poolGamblerScores: [PoolGamblerScoreModel] = (1...50).lazy.map { _ in
        poolGamblerScorePlaceholderModel()
    }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: boxSpacing.medium) {
                ForEach(poolGamblerScores) { poolGamblerScore in
                    GamblerScoreItem(
                        poolGamblerScore: poolGamblerScore,
                        isCurrentUser: false
                    )
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
                pagingQuery: { _ in
                        .success(CursorPage(items: poolGamblerScoresDummyModels(), next: nil))
                }
            )
        )
    )

    GamblerScoreList(
        lazyPager: lazyPager,
        isCurrentUser: "signed-in-gambler-id"
    )
    .onAppearOnce {
        lazyPager.refresh()
    }
}
