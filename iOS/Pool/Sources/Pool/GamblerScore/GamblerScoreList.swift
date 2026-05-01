import SwiftUI
import Core
import UI

struct GamblerScoreList: View {
    var lazyPagingItems: LazyPagingItems<String, PoolGamblerScoreModel>
    let isCurrentUser: String?
    let onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)?

    init(
        lazyPagingItems: LazyPagingItems<String, PoolGamblerScoreModel>,
        isCurrentUser: String?,
        onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)? = nil
    ) {
        self.lazyPagingItems = lazyPagingItems
        self.isCurrentUser = isCurrentUser
        self.onGamblerOpen = onGamblerOpen
    }

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        let _ = Self._printChangesIfDebug()

        RefreshableStatefulLazyVStack(
            lazyPagingItems: lazyPagingItems,
            loadingContent: { GamblerScorePlaceholderList() },
            loadingContentOnConcatenate: {
                VStack(spacing: 0) {
                    PoolScoreItem(poolGamblerScore: poolGamblerScorePlaceholderModel(), onJoin: {})
                        .shimmer()
                    Divider()
                }
                .padding(.horizontal, boxSpacing.medium)
            },
            errorContent: { error in
                StatefulLazyVStackError(localizedError: error.localizedErrorOrDefault())
                    .padding(boxSpacing.medium)
            },
            emptyContent: { StatefulLazyVStackEmpty().padding(boxSpacing.medium) },
        ) { poolGamblerScore in
            if let onGamblerOpen {
                VStack(spacing: 0) {
                    GamblerScoreItem(
                        poolGamblerScore: poolGamblerScore,
                        isCurrentUser: isCurrentUser != nil ? isCurrentUser == poolGamblerScore.gamblerId : false
                    )
                    .padding(boxSpacing.medium)
                    Divider()
                }
                .padding(.horizontal, boxSpacing.medium)
                .contentShape(Rectangle())
                .onTapGesture {
                    onGamblerOpen(
                        poolGamblerScore.poolId,
                        poolGamblerScore.gamblerId,
                        poolGamblerScore.gamblerUsername
                    )
                }
            } else {
                VStack(spacing: 0) {
                    GamblerScoreItem(
                        poolGamblerScore: poolGamblerScore,
                        isCurrentUser: isCurrentUser != nil ? isCurrentUser == poolGamblerScore.gamblerId : false
                    )
                    .padding(boxSpacing.medium)
                    Divider()
                }
                .padding(.horizontal, boxSpacing.medium)
            }
        }
    }
}

struct GamblerScorePlaceholderList: View {
    @Environment(\.boxSpacing) private var boxSpacing

    private let poolGamblerScores: [PoolGamblerScoreModel] = (1...50).lazy.map { _ in
        poolGamblerScorePlaceholderModel()
    }

    var body: some View {
        ForEach(poolGamblerScores) { poolGamblerScore in
            VStack(spacing: 0) {
                GamblerScoreItem(
                    poolGamblerScore: poolGamblerScore,
                    isCurrentUser: false
                )
                .shimmer()
                .padding(boxSpacing.medium)
                Divider()
            }
            .padding(.horizontal, boxSpacing.medium)
        }
    }
}

#Preview("List") {
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

#Preview("Placeholder") {
    ScrollView {
        LazyVStack(spacing: 0) {
            GamblerScorePlaceholderList()
        }
    }
}
