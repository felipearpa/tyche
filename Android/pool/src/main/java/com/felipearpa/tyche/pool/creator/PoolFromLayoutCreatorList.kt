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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.felipearpa.tyche.ui.lazy.RefreshableLazyPagingColumn
import com.felipearpa.tyche.ui.shimmer
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
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
    RefreshableLazyPagingColumn(
        modifier = modifier,
        lazyPagingItems = poolLayouts,
        lazyListState = lazyListState,
        loadingContent = { poolFromLayoutCreatorPlaceholderList(count = fakeItemCount) },
        appendLoadingContent = {
            item { PoolFromLayoutCreatorFakeItem(modifier = Modifier.poolFromLayoutCreatorItem()) }
        },
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
    ) {
        items(
            count = poolLayouts.itemCount,
            key = poolLayouts.itemKey { poolLayout -> poolLayout.id },
            contentType = poolLayouts.itemContentType { "PoolLayout" },
        ) { index ->
            val poolLayout = poolLayouts[index] ?: return@items
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

private fun LazyListScope.poolFromLayoutCreatorPlaceholderList(count: Int) {
    repeat(count) {
        item {
            PoolFromLayoutCreatorFakeItem(modifier = Modifier.poolFromLayoutCreatorItem())
        }
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

@PreviewLightDark
@Composable
private fun PoolFromLayoutCreatorListPreview() {
    val items =
        MutableStateFlow(PagingData.from(poolLayoutDummyModels())).collectAsLazyPagingItems()
    TycheTheme {
        Surface {
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
    }
}

@PreviewLightDark
@Composable
private fun PoolFromLayoutCreatorListFakePreview() {
    TycheTheme {
        Surface {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(LocalBoxSpacing.current.medium),
            ) {
                poolFromLayoutCreatorPlaceholderList(count = 3)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PoolFromLayoutCreatorFakeItemPreview() {
    TycheTheme {
        Surface {
            PoolFromLayoutCreatorFakeItem(modifier = Modifier.fillMaxWidth())
        }
    }
}
