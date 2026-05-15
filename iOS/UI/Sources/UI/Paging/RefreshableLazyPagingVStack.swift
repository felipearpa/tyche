import SwiftUI
import LazyPaging

public extension LazyPaging.RefreshableLazyPagingVStack where EmptyContent == LazyPagingVStackEmpty, ErrorContent == LazyPagingVStackError {
    init(
        lazyPagingItems: LazyPaging.LazyPagingItems<Key, Item>,
        spacing: CGFloat = 0,
        contentInsets: EdgeInsets = EdgeInsets(),
        pinnedViews: PinnedScrollableViews = [],
        @ViewBuilder loadingContent: @escaping () -> LoadingContent,
        @ViewBuilder prependLoadingContent: @escaping () -> PrependLoadingContent,
        @ViewBuilder appendLoadingContent: @escaping () -> AppendLoadingContent,
        @ViewBuilder prependErrorContent: @escaping (any Error) -> PrependErrorContent,
        @ViewBuilder appendErrorContent: @escaping (any Error) -> AppendErrorContent,
        @ViewBuilder rowContent: @escaping (Int) -> RowContent
    ) {
        self.init(
            lazyPagingItems: lazyPagingItems,
            spacing: spacing,
            contentInsets: contentInsets,
            pinnedViews: pinnedViews,
            loadingContent: loadingContent,
            emptyContent: { LazyPagingVStackEmpty() },
            errorContent: { error in LazyPagingVStackError(localizedError: error.orDefaultLocalized()) },
            prependLoadingContent: prependLoadingContent,
            appendLoadingContent: appendLoadingContent,
            prependErrorContent: prependErrorContent,
            appendErrorContent: appendErrorContent,
            rowContent: rowContent
        )
    }
}

public extension LazyPaging.RefreshableLazyPagingVStack
where
    EmptyContent == LazyPagingVStackEmpty,
    ErrorContent == LazyPagingVStackError,
    PrependLoadingContent == EmptyView,
    PrependErrorContent == EmptyView,
    AppendErrorContent == EmptyView
{
    init(
        lazyPagingItems: LazyPaging.LazyPagingItems<Key, Item>,
        spacing: CGFloat = 0,
        contentInsets: EdgeInsets = EdgeInsets(),
        pinnedViews: PinnedScrollableViews = [],
        @ViewBuilder loadingContent: @escaping () -> LoadingContent,
        @ViewBuilder appendLoadingContent: @escaping () -> AppendLoadingContent,
        @ViewBuilder rowContent: @escaping (Int) -> RowContent
    ) {
        self.init(
            lazyPagingItems: lazyPagingItems,
            spacing: spacing,
            contentInsets: contentInsets,
            pinnedViews: pinnedViews,
            loadingContent: loadingContent,
            emptyContent: { LazyPagingVStackEmpty() },
            errorContent: { error in LazyPagingVStackError(localizedError: error.orDefaultLocalized()) },
            prependLoadingContent: { EmptyView() },
            appendLoadingContent: appendLoadingContent,
            prependErrorContent: { _ in EmptyView() },
            appendErrorContent: { _ in EmptyView() },
            rowContent: rowContent
        )
    }
}

public extension LazyPaging.RefreshableLazyPagingVStack where EmptyContent == LazyPagingVStackEmpty, ErrorContent == LazyPagingVStackError {
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
