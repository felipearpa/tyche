import SwiftUI
import LazyPaging

public extension LazyPaging.LazyPagingVStack where EmptyContent == LazyPagingVStackEmpty, ErrorContent == LazyPagingVStackError {
    init(
        lazyPagingItems: LazyPaging.LazyPagingItems<Key, Item>,
        spacing: CGFloat = 0,
        contentInsets: EdgeInsets = EdgeInsets(),
        pinnedViews: PinnedScrollableViews = [],
        @ViewBuilder loadingContent: @escaping () -> LoadingContent,
        @ViewBuilder rowContent: @escaping (Int) -> RowContent
    ) where
        PrependLoadingContent == EmptyView,
        AppendLoadingContent == EmptyView,
        PrependErrorContent == EmptyView,
        AppendErrorContent == EmptyView
    {
        self.init(
            lazyPagingItems: lazyPagingItems,
            spacing: spacing,
            contentInsets: contentInsets,
            pinnedViews: pinnedViews,
            loadingContent: loadingContent,
            emptyContent: { LazyPagingVStackEmpty() },
            errorContent: { error in LazyPagingVStackError(localizedError: error.orDefaultLocalized()) },
            rowContent: rowContent
        )
    }
}
