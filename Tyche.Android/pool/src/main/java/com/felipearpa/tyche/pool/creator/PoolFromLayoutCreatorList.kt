package com.felipearpa.tyche.pool.creator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.felipearpa.tyche.ui.lazy.RefreshableStatefulLazyColumn
import com.felipearpa.tyche.ui.shimmer
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import kotlinx.coroutines.flow.flowOf

@Composable
fun PoolFromLayoutCreatorList(
    modifier: Modifier = Modifier,
    poolLayouts: LazyPagingItems<PoolLayoutModel>,
    lazyListState: LazyListState = rememberLazyListState(),
    fakeItemCount: Int,
) {
    RefreshableStatefulLazyColumn(
        modifier = modifier,
        lazyItems = poolLayouts,
        state = lazyListState,
        loadingContent = {
            PoolFromLayoutCreatorFakeList(
                modifier = modifier,
                count = fakeItemCount,
            )
        },
        loadingContentOnConcatenate = { poolFromLayoutCreatorFakeItem() },
    ) {
        items(
            count = poolLayouts.itemCount,
            key = poolLayouts.itemKey { poolLayout -> poolLayout.id },
            contentType = poolLayouts.itemContentType { "PoolLayout" },
        ) { index ->
            poolLayouts[index]?.let { poolLayout ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    PoolFromLayoutCreatorItem(
                        poolLayout = poolLayout,
                        modifier = Modifier.poolFromLayoutCreatorItem(),
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun PoolFromLayoutCreatorFakeList(modifier: Modifier = Modifier, count: Int) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(count) {
            poolFromLayoutCreatorFakeItem()
        }
    }
}

private fun LazyListScope.poolFromLayoutCreatorFakeItem() {
    item {
        PoolFromLayoutCreatorFakeItem(modifier = Modifier.poolFromLayoutCreatorItem())
        HorizontalDivider()
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
        .padding(all = LocalBoxSpacing.current.medium)

@Preview(showBackground = true)
@Composable
private fun PoolFromLayoutCreatorListPreview() {
    val items = flowOf(PagingData.from(poolLayoutDummyModels())).collectAsLazyPagingItems()
    PoolFromLayoutCreatorList(
        modifier = Modifier
            .fillMaxSize()
            .padding(LocalBoxSpacing.current.medium),
        poolLayouts = items,
        fakeItemCount = 50,
    )
}

@Preview(showBackground = true)
@Composable
private fun PoolFromLayoutCreatorListFakePreview() {
    PoolFromLayoutCreatorFakeList(
        modifier = Modifier
            .fillMaxSize()
            .padding(LocalBoxSpacing.current.medium),
        count = 50,
    )
}

@Preview(showBackground = true)
@Composable
private fun PoolFromLayoutCreatorFakeItemPreview() {
    PoolFromLayoutCreatorFakeItem(modifier = Modifier.fillMaxWidth())
}
