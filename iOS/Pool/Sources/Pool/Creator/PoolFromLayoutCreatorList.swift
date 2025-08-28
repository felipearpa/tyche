import SwiftUI
import Core
import UI

struct PoolFromLayoutCreatorList: View {
    var lazyPagingItems: LazyPagingItems<String, PoolLayoutModel>
    var fakeItemCount: Int
    var selectedPoolLayout: PoolLayoutModel?
    var onPoolLayoutChange: (PoolLayoutModel) -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        StatefulLazyVStack(
            lazyPagingItems: lazyPagingItems,
            loadingContent: { PoolFromLayoutCreatorPlaceholderList(count: fakeItemCount) },
            loadingContentOnConcatenate: {
                PoolFromLayoutCreatorItem(
                    poolLayout: poolLayoutFakeModel(),
                    isSelected: false
                )
                .shimmer()
            },
            spacing: boxSpacing.small,
        ) { poolLayout in
            PoolFromLayoutCreatorItem(
                poolLayout: poolLayout,
                isSelected: poolLayout.id == selectedPoolLayout?.id
            )
            .onTapGesture {
                onPoolLayoutChange(poolLayout)
            }
        }
    }
}

private struct PoolFromLayoutCreatorPlaceholderList: View {
    let count: Int

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        ForEach(0..<count, id: \.self) { _ in
            PoolFromLayoutCreatorItem(
                poolLayout: poolLayoutFakeModel(),
                isSelected: false
            )
            .shimmer()
        }
    }
}

#Preview("PoolFromLayoutCreatorList") {
    PoolFromLayoutCreatorList(
        lazyPagingItems: LazyPagingItems(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: {
                    OpenPoolLayoutPagingSource(
                        pagingQuery: { _ in .success(CursorPage(items: poolLayoutDummyModels(), next: nil))
                        }
                    )
                }
            )
        ),
        fakeItemCount: 3,
        selectedPoolLayout: nil,
        onPoolLayoutChange: { _ in },
    )
}

#Preview("PoolFromLayoutCreatorPlaceholderList") {
    PoolFromLayoutCreatorPlaceholderList(count: 5)
}
