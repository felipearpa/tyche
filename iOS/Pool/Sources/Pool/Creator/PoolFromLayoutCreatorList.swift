import SwiftUI
import Core
import UI

struct PoolFromLayoutCreatorList: View {
    var lazyPager: LazyPager<String, PoolLayoutModel>
    var fakeItemCount: Int
    var selectedPoolLayout: PoolLayoutModel?
    var onPoolLayoutChange: (PoolLayoutModel) -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        PagingVStack(
            lazyPager: lazyPager,
            loadingContent: {
                PoolFromLayoutCreatorPlaceholderList(count: fakeItemCount)
            },
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
        ScrollView {
            LazyVStack(spacing: boxSpacing.small) {
                ForEach(0..<count, id: \.self) { _ in
                    PoolFromLayoutCreatorItem(
                        poolLayout: poolLayoutFakeModel(),
                        isSelected: false
                    )
                    .shimmer()
                }
            }
        }
    }
}

#Preview("PoolFromLayoutCreatorList") {
    let lazyPager = LazyPager(
        pagingData: PagingData(
            pagingConfig: PagingConfig(prefetchDistance: 5),
            pagingSourceFactory: OpenPoolLayoutPagingSource(
                pagingQuery: { _ in
                    .success(CursorPage(items: poolLayoutDummyModels(), next: nil))
                }
            )
        )
    )

    PoolFromLayoutCreatorList(
        lazyPager: lazyPager,
        fakeItemCount: 5,
        selectedPoolLayout: nil,
        onPoolLayoutChange: { _ in },
    )
    .onAppear {
        lazyPager.refresh()
    }
}

#Preview("PoolFromLayoutCreatorPlaceholderList") {
    PoolFromLayoutCreatorPlaceholderList(count: 5)
}
