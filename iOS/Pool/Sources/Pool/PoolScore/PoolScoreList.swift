import SwiftUI
import Core
import UI

struct PoolScoreList: View {
    let lazyPagingItems: LazyPagingItems<String, PoolGamblerScoreModel>
    let onPoolOpen: (String) -> Void
    let onPoolJoin: (String) -> Void
    let onPoolCreate: () -> Void

    var body: some View {
        StatefulLazyVStack(
            lazyPagingItems: lazyPagingItems,
            loadingContent: { PoolScorePlaceholderList() },
            loadingContentOnConcatenate: {
                PoolScoreItem(poolGamblerScore: poolGamblerScorePlaceholderModel(), onJoin: {})
                    .shimmer()
                Divider()
            },
            errorContent: { error in StatefulLazyVStackError(localizedError: error.localizedErrorOrDefault()) },
            emptyContent: { PoolScoreEmptyList(onPoolCreate: onPoolCreate) },
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

private struct PoolScorePlaceholderList : View {
    private let poolGamblerScores: [PoolGamblerScoreModel] = (1...50).lazy.map { _ in
        poolGamblerScorePlaceholderModel()
    }

    var body: some View {
        ForEach(poolGamblerScores) { poolGamblerScore in
            PoolScoreItem(poolGamblerScore: poolGamblerScore, onJoin: {})
                .shimmer()
            Divider()
        }
    }
}

private struct PoolScoreEmptyList: View {
    let onPoolCreate: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
            Image(.emojiPeople)
                .resizable()
                .scaledToFit()
                .frame(width: iconSize, height: iconSize)
                .foregroundColor(.primary)

            Text(String(.poolScoreEmptyListTitle))
                .font(.title3)
                .multilineTextAlignment(.center)

            Text(String(.poolScoreEmptyListSubtitle))
                .font(.footnote)
                .multilineTextAlignment(.center)
                .foregroundColor(.secondary)

            Spacer().frame(height: boxSpacing.medium)

            Button(action: onPoolCreate) {
                Text(String(.createPoolAction))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
        }
        .padding(boxSpacing.medium)
    }
}

private let iconSize: CGFloat = 64

#Preview("PoolScoreList") {
    PoolScoreList(
        lazyPagingItems: LazyPagingItems(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: {
                    PoolGamblerScorePagingSource(
                        pagingQuery: { _ in .success(CursorPage(items: poolGamblerScoresDummyModels(), next: nil)) }
                    )
                }
            )
        ),
        onPoolOpen: { _ in },
        onPoolJoin: { _ in },
        onPoolCreate: {},
    )
}

#Preview("PoolScoreEmptyList") {
    PoolScoreList(
        lazyPagingItems: LazyPagingItems(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: {
                    PoolGamblerScorePagingSource(
                        pagingQuery: { _ in .success(CursorPage(items: [], next: nil)) }
                    )
                }
            )
        ),
        onPoolOpen: { _ in },
        onPoolJoin: { _ in },
        onPoolCreate: {},
    )
}

#Preview("PoolScorePlaceholderList") {
    PoolScorePlaceholderList()
}
