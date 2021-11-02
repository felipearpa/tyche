package com.pipel.tyche.ui.column

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.pipel.ui.DividerDecorator

@Composable
fun <TModel : Any> RefreshableLazyColumn(
    modifier: Modifier = Modifier,
    lazyItems: LazyPagingItems<TModel>,
    filterContent: @Composable (() -> Unit)? = null,
    onLoadingContent: @Composable () -> Unit,
    itemContent: @Composable (TModel) -> Unit
) {
    com.pipel.ui.RefreshableLazyColumn(
        modifier = modifier,
        lazyItems = lazyItems,
        filterContent = filterContent,
        onLoadingContent = onLoadingContent,
        onLoadingAppendContent = {
            DividerDecorator(bottom = 8.dp) {
                ColumnProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        },
        onErrorAppendContent = {
            DividerDecorator(bottom = 8.dp) {
                ColumnRetry(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) { lazyItems.retry() }
            }
        },
        onErrorContent = {
            DividerDecorator(bottom = 8.dp) {
                ColumnError(modifier = Modifier.fillMaxWidth()) {
                    lazyItems.retry()
                }
            }
        },
        onEmptyContent = {
            DividerDecorator(bottom = 8.dp) {
                ColumnEmpty(modifier = Modifier.fillMaxWidth())
            }
        },
        itemContent = { item ->
            DividerDecorator(bottom = 8.dp) {
                Spacer(modifier = Modifier.height(8.dp))
                itemContent(item)
            }
        }
    )
}