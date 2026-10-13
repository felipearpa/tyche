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

        let contentInsets = EdgeInsets(top: boxSpacing.medium, leading: 0, bottom: boxSpacing.medium, trailing: 0)
        AppendPagingVStack(
            lazyPagingItems: lazyPagingItems,
            // The spacing goes inside the scroll view: an outer padding would keep it off the
            // bars' safe-area edges, so rows would stop short of them instead of scrolling beneath.
            contentInsets: contentInsets,
            loadingContent: { GamblerScorePlaceholderList() },
            emptyContent: { LazyPagingVStackEmpty() },
            errorContent: { error in
                LazyPagingVStackError(
                    localizedError: error.orDefaultLocalized(),
                    retry: { Task { await lazyPagingItems.refresh() } }
                )
            },
            appendLoadingContent: { GamblerScorePlaceholderRow() },
            appendErrorContent: { error in
                LazyPagingVStackConcatenateError(
                    localizedError: error.localizedErrorOrDefault(),
                    retry: { Task { await lazyPagingItems.retry() } }
                )
            }
        ) { index in
            if let poolGamblerScore = lazyPagingItems.peek(at: index) {
                let isSignedInGambler =
                    isCurrentUser != nil
                    && isCurrentUser == poolGamblerScore.gamblerId

                if let onGamblerOpen, !isSignedInGambler {
                    Button {
                        onGamblerOpen(
                            poolGamblerScore.poolId,
                            poolGamblerScore.gamblerId,
                            poolGamblerScore.gamblerUsername
                        )
                    } label: {
                        scoreRow(
                            poolGamblerScore,
                            isCurrentUser: false
                        )
                    }
                    .buttonStyle(InteractiveRowButtonStyle())
                } else {
                    scoreRow(
                        poolGamblerScore,
                        isCurrentUser: isSignedInGambler
                    )
                }
            }
        }
        .refreshable { await lazyPagingItems.refresh() }
        .fullListStateViewport(contentInsets: contentInsets)
    }

    private func scoreRow(
        _ poolGamblerScore: PoolGamblerScoreModel,
        isCurrentUser: Bool
    ) -> some View {
        VStack(spacing: 0) {
            GamblerScoreItem(
                poolGamblerScore: poolGamblerScore,
                isCurrentUser: isCurrentUser
            )
            Divider()
        }
        .padding(.horizontal, boxSpacing.medium)
        .frame(maxWidth: .infinity)
    }
}

struct GamblerScorePlaceholderList: View {
    private let poolGamblerScores: [PoolGamblerScoreModel] = (1...50).lazy.map { _ in
        poolGamblerScorePlaceholderModel()
    }

    var body: some View {
        ForEach(poolGamblerScores) { poolGamblerScore in
            GamblerScorePlaceholderRow(poolGamblerScore: poolGamblerScore)
        }
    }
}

/// Initial and append loading row. It is never wrapped in the gambler-detail `Button` that
/// loaded rows use, so the placeholder has no parent activation target, and the row itself
/// ignores touches and stays out of the accessibility tree.
struct GamblerScorePlaceholderRow: View {
    let poolGamblerScore: PoolGamblerScoreModel

    @Environment(\.boxSpacing) private var boxSpacing

    init(poolGamblerScore: PoolGamblerScoreModel = poolGamblerScorePlaceholderModel()) {
        self.poolGamblerScore = poolGamblerScore
    }

    var body: some View {
        VStack(spacing: 0) {
            GamblerScoreItem(
                poolGamblerScore: poolGamblerScore,
                isCurrentUser: false,
                isPlaceholder: true
            )
            Divider()
        }
        .padding(.horizontal, boxSpacing.medium)
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
