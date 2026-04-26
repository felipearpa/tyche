import SwiftUI
import Core
import UI

struct PoolScoreList: View {
    let lazyPagingItems: LazyPagingItems<String, PoolGamblerScoreModel>
    let lazyPoolLayouts: LazyPagingItems<String, PoolLayoutModel>
    let onPoolOpen: (String) -> Void
    let onPoolJoin: (String) -> Void
    let onPoolLayoutSelect: (PoolLayoutModel) -> Void
    let onSeeAllTemplates: () -> Void

    @State var rootSize: CGSize = .zero

    var body: some View {
        let _ = Self._printChangesIfDebug()

        RefreshableStatefulLazyVStack(
            lazyPagingItems: lazyPagingItems,
            loadingContent: { PoolScorePlaceholderList() },
            loadingContentOnConcatenate: {
                PoolScoreItem(poolGamblerScore: poolGamblerScorePlaceholderModel(), onJoin: {})
                    .shimmer()
                Divider()
            },
            errorContent: { error in PoolScoreErrorList(error: error) },
            emptyContent: {
                PoolScoreEmptyList(
                    lazyPoolLayouts: lazyPoolLayouts,
                    onPoolLayoutSelect: onPoolLayoutSelect,
                    onSeeAllTemplates: onSeeAllTemplates,
                )
            },
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
    @ObservedObject var lazyPoolLayouts: LazyPagingItems<String, PoolLayoutModel>
    let onPoolLayoutSelect: (PoolLayoutModel) -> Void
    let onSeeAllTemplates: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Hero()
                .padding(.horizontal, emptyStateHorizontalPadding)
                .padding(.vertical, emptyStateHeroVerticalPadding)
                .frame(maxWidth: .infinity)

            Text(String(.poolScoreEmptyListTemplatesSection))
                .font(.headline)
                .padding(.horizontal, emptyStateHorizontalPadding)
                .padding(.bottom, boxSpacing.medium)

            TemplatesContent(
                lazyPoolLayouts: lazyPoolLayouts,
                onPoolLayoutSelect: onPoolLayoutSelect,
                onSeeAllTemplates: onSeeAllTemplates,
            )
        }
        .task { await lazyPoolLayouts.refresh() }
    }
}

private struct Hero: View {
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.large) {
            Image(.peoplePlaying)
                .resizable()
                .scaledToFit()
                .frame(width: heroImageWidth, height: heroImageHeight)

            VStack(spacing: boxSpacing.medium) {
                Text(String(.poolScoreEmptyListTitle))
                    .font(.title2)
                    .multilineTextAlignment(.center)

                Text(String(.poolScoreEmptyListSubtitle))
                    .font(.callout)
                    .multilineTextAlignment(.center)
                    .foregroundColor(.secondary)
            }
        }
    }
}

private struct TemplatesContent: View {
    @ObservedObject var lazyPoolLayouts: LazyPagingItems<String, PoolLayoutModel>
    let onPoolLayoutSelect: (PoolLayoutModel) -> Void
    let onSeeAllTemplates: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        if lazyPoolLayouts.loadState.refresh.isLoading {
            VStack(spacing: boxSpacing.medium) {
                ForEach(0..<popularTemplatesCount, id: \.self) { _ in
                    PoolFromLayoutCreatorItem(poolLayout: poolLayoutFakeModel(), isSelected: false)
                        .shimmer()
                }
            }
            .padding(.horizontal, emptyStateHorizontalPadding)
        } else if case .failure(let error, _) = lazyPoolLayouts.loadState.refresh {
            StatefulLazyVStackError(localizedError: error.localizedErrorOrDefault())
                .padding(.horizontal, emptyStateHorizontalPadding)
                .padding(.vertical, boxSpacing.medium)
        } else {
            let visibleLayouts = Array(lazyPoolLayouts.prefix(popularTemplatesCount))

            VStack(spacing: boxSpacing.medium) {
                ForEach(visibleLayouts) { poolLayout in
                    PoolFromLayoutCreatorItem(poolLayout: poolLayout, isSelected: false)
                        .onTapGesture {
                            onPoolLayoutSelect(poolLayout)
                        }
                }
            }
            .padding(.horizontal, emptyStateHorizontalPadding)

            if !visibleLayouts.isEmpty {
                HStack {
                    Spacer()
                    Button(action: onSeeAllTemplates) {
                        Text(String(.seeAllTemplatesAction))
                    }
                    Spacer()
                }
                .padding(.horizontal, emptyStateHorizontalPadding)
                .padding(.top, boxSpacing.medium)
                .padding(.bottom, emptyStateHeroVerticalPadding)
            }
        }
    }
}

private struct PoolScoreErrorList: View {
    let error: Error

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        StatefulLazyVStackError(localizedError: error.localizedErrorOrDefault())
            .padding(boxSpacing.medium)
    }
}

private let heroImageWidth: CGFloat = 240
private let heroImageHeight: CGFloat = 160
private let emptyStateHorizontalPadding: CGFloat = 16
private let emptyStateHeroVerticalPadding: CGFloat = 32
private let popularTemplatesCount: Int = 3

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
        lazyPoolLayouts: LazyPagingItems(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: {
                    OpenPoolLayoutPagingSource(
                        pagingQuery: { _ in .success(CursorPage(items: poolLayoutDummyModels(), next: nil)) }
                    )
                }
            )
        ),
        onPoolOpen: { _ in },
        onPoolJoin: { _ in },
        onPoolLayoutSelect: { _ in },
        onSeeAllTemplates: {},
    )
}

@ViewBuilder
private func poolScoreEmptyListPreview() -> some View {
    NavigationStack {
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
            lazyPoolLayouts: LazyPagingItems(
                pagingData: PagingData(
                    pagingConfig: PagingConfig(prefetchDistance: 5),
                    pagingSourceFactory: {
                        OpenPoolLayoutPagingSource(
                            pagingQuery: { _ in .success(CursorPage(items: poolLayoutDummyModels(), next: nil)) }
                        )
                    }
                )
            ),
            onPoolOpen: { _ in },
            onPoolJoin: { _ in },
            onPoolLayoutSelect: { _ in },
            onSeeAllTemplates: {},
        )
        .withParentGeometryProxy()
    }
}

#Preview("PoolScoreEmptyList - Light") {
    poolScoreEmptyListPreview()
        .preferredColorScheme(.light)
}

#Preview("PoolScoreEmptyList - Dark") {
    poolScoreEmptyListPreview()
        .preferredColorScheme(.dark)
}

#Preview("PoolScorePlaceholderList") {
    PoolScorePlaceholderList()
}
