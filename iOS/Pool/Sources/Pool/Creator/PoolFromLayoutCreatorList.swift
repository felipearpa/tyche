import SwiftUI
import Core
import UI
import LazyPaging

struct PoolFromLayoutCreatorList: View {
    var lazyPagingItems: LazyPaging.LazyPagingItems<String, PoolLayoutModel>
    var fakeItemCount: Int
    var selectedPoolLayout: PoolLayoutModel?
    var onPoolLayoutChange: (PoolLayoutModel) -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        RefreshableLazyPagingVStack(
            lazyPagingItems: lazyPagingItems,
            spacing: boxSpacing.small,
            loadingContent: { PoolFromLayoutCreatorPlaceholderList(count: fakeItemCount) },
            appendLoadingContent: {
                PoolFromLayoutCreatorItem(
                    poolLayout: poolLayoutFakeModel(),
                    isSelected: false
                )
                .shimmer()
            },
        ) { index in
            if let poolLayout = lazyPagingItems.peek(at: index) {
                PoolFromLayoutCreatorItem(
                    poolLayout: poolLayout,
                    isSelected: poolLayout.id == selectedPoolLayout?.id
                )
                .onTapGesture {
                    onPoolLayoutChange(poolLayout)
                }
            } else {
                PoolFromLayoutCreatorItem(
                    poolLayout: poolLayoutFakeModel(),
                    isSelected: false
                )
                .shimmer()
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
        lazyPagingItems: LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 25, prefetchDistance: 5),
                pagingSourceFactory: {
                    LazyPagingCursorSource<PoolLayoutModel>(
                        pagingQuery: { _ in .success(CursorPage(items: poolLayoutDummyModels(), next: nil)) }
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
