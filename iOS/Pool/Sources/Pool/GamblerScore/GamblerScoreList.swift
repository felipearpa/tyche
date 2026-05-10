import SwiftUI
import Core
import UI
import LazyPaging

struct GamblerScoreList: View {
    var lazyPagingItems: LazyPaging.LazyPagingItems<String, PoolGamblerScoreModel>
    let isCurrentUser: String?
    let onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)?

    init(
        lazyPagingItems: LazyPaging.LazyPagingItems<String, PoolGamblerScoreModel>,
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

        RefreshableLazyPagingVStack(
            lazyPagingItems: lazyPagingItems,
            loadingContent: { GamblerScorePlaceholderList() },
        ) { index in
            if let poolGamblerScore = lazyPagingItems.peek(at: index) {
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
            } else {
                VStack(spacing: 0) {
                    GamblerScoreItem(
                        poolGamblerScore: poolGamblerScorePlaceholderModel(),
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
    let lazyPagingItems = LazyPaging.LazyPagingItems(
        pager: Pager(
            config: LazyPaging.PagingConfig(pageSize: 25, prefetchDistance: 5),
            pagingSourceFactory: {
                LazyPagingCursorSource<PoolGamblerScoreModel>(
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
