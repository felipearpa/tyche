import SwiftUI
import Core
import UI

struct GamblerScoreList: View {
    var lazyPagingItems: LazyPagingItems<String, PoolGamblerScoreModel>
    let isCurrentUser: String?

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        let _ = Self._printChangesIfDebug()

        StatefulLazyVStack(
            lazyPagingItems: lazyPagingItems,
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

#Preview {
    let lazyPagingItems = LazyPagingItems(
        pagingData: PagingData(
            pagingConfig: PagingConfig(prefetchDistance: 5),
            pagingSourceFactory: {
                PoolGamblerScorePagingSource(
                    pagingQuery: { _ in .success(CursorPage(items: poolGamblerScoresDummyModels(), next: nil)) }
                )
            }
        )
    )

    GamblerScoreList(
        lazyPagingItems: lazyPagingItems,
        isCurrentUser: "signed-in-gambler-id"
    )
}
