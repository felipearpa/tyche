package com.pipel.tyche.ui.column

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.pipel.ui.column.RefreshableLazyColumn

@Composable
fun <TModel : Any> RefreshableLazyColumn(
    lazyItems: LazyPagingItems<TModel>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Bottom,
    filterContent: @Composable (() -> Unit)? = null,
    onLoadingContent: @Composable () -> Unit,
    itemContent: @Composable (TModel) -> Unit
) {
    RefreshableLazyColumn(
        modifier = modifier,
        lazyItems = lazyItems,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        filterContent = filterContent,
        onLoadingContent = onLoadingContent,
        onLoadingAppendContent = {
            ColumnProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        },
        onErrorAppendContent = {
            ColumnRetry(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) { lazyItems.retry() }
        },
        onErrorContent = {
            ColumnError(modifier = Modifier.fillMaxWidth()) {
                lazyItems.retry()
            }
        },
        onEmptyContent = {
            ColumnEmpty(modifier = Modifier.fillMaxWidth())
        },
        itemContent = { item ->
            itemContent(item)
        }
    )
}