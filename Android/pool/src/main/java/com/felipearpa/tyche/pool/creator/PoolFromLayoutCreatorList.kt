package com.felipearpa.tyche.pool.creator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.felipearpa.tyche.ui.lazy.RefreshableStatefulLazyColumn
import com.felipearpa.tyche.ui.shimmer
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun PoolFromLayoutCreatorList(
    poolLayouts: LazyPagingItems<PoolLayoutModel>,
    fakeItemCount: Int,
    selectedPoolLayout: PoolLayoutModel?,
    onPoolLayoutChange: (PoolLayoutModel) -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    RefreshableStatefulLazyColumn(
        modifier = modifier,
        lazyPagingItems = poolLayouts,
        state = lazyListState,
        loadingContent = { poolFromLayoutCreatorPlaceholderList(count = fakeItemCount) },
        loadingContentOnConcatenate = { poolFromLayoutCreatorPlaceholderItem() },
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
    ) {
        items(
            count = poolLayouts.itemCount,
            key = poolLayouts.itemKey { poolLayout -> poolLayout.id },
            contentType = poolLayouts.itemContentType { "PoolLayout" },
        ) { index ->
            poolLayouts[index]?.let { poolLayout ->
                PoolFromLayoutCreatorItem(
                    poolLayout = poolLayout,
                    modifier = Modifier
                        .poolFromLayoutCreatorItem()
                        .clickable { onPoolLayoutChange(poolLayout) },
                    isSelected = poolLayout == selectedPoolLayout,
                )
            }
        }
    }
}

private fun LazyListScope.poolFromLayoutCreatorPlaceholderList(count: Int) {
    repeat(count) {
        poolFromLayoutCreatorPlaceholderItem()
    }
}

private fun LazyListScope.poolFromLayoutCreatorPlaceholderItem() {
    item {
        PoolFromLayoutCreatorFakeItem(modifier = Modifier.poolFromLayoutCreatorItem())
    }
}

@Composable
private fun PoolFromLayoutCreatorFakeItem(modifier: Modifier = Modifier) {
    PoolFromLayoutCreatorItem(
        poolLayout = poolLayoutFakeModel(),
        modifier = modifier,
        shimmerModifier = Modifier.shimmer(),
    )
}

@Composable
private fun Modifier.poolFromLayoutCreatorItem() =
    fillMaxWidth()

@Preview(showBackground = true)
@Composable
private fun PoolFromLayoutCreatorListPreview() {
    val items =
        MutableStateFlow(PagingData.from(poolLayoutDummyModels())).collectAsLazyPagingItems()
    PoolFromLayoutCreatorList(
        modifier = Modifier
            .fillMaxSize()
            .padding(LocalBoxSpacing.current.medium),
        poolLayouts = items,
        fakeItemCount = 5,
        selectedPoolLayout = null,
        onPoolLayoutChange = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PoolFromLayoutCreatorListFakePreview() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(LocalBoxSpacing.current.medium),
    ) {
        poolFromLayoutCreatorPlaceholderList(count = 3)
    }
}

@Preview(showBackground = true)
@Composable
private fun PoolFromLayoutCreatorFakeItemPreview() {
    PoolFromLayoutCreatorFakeItem(modifier = Modifier.fillMaxWidth())
}
