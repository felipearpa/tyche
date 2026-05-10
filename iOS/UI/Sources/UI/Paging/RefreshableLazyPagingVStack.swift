import SwiftUI
import LazyPaging

public extension LazyPaging.RefreshableLazyPagingVStack where EmptyContent == LazyPagingColumnEmpty, ErrorContent == LazyPagingColumnError {
    init(
        lazyPagingItems: LazyPaging.LazyPagingItems<Key, Item>,
        spacing: CGFloat = 0,
        contentInsets: EdgeInsets = EdgeInsets(),
        pinnedViews: PinnedScrollableViews = [],
        @ViewBuilder loadingContent: @escaping () -> LoadingContent,
        @ViewBuilder rowContent: @escaping (Int) -> RowContent
    ) {
        self.init(
            lazyPagingItems: lazyPagingItems,
            spacing: spacing,
            contentInsets: contentInsets,
            pinnedViews: pinnedViews,
            loadingContent: loadingContent,
            emptyContent: { LazyPagingColumnEmpty() },
            errorContent: { error in LazyPagingColumnError(localizedError: error.orDefaultLocalized()) },
            rowContent: rowContent
        )
    }
}
