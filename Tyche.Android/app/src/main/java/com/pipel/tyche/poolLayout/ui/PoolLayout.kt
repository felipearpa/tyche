package com.pipel.tyche.poolLayout.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.pipel.core.empty
import com.pipel.tyche.poolLayout.view.PoolLayoutModel
import com.pipel.tyche.ui.Filter
import com.pipel.tyche.ui.column.RefreshableLazyColumn

@Composable
fun PoolLayoutView(viewModel: PoolLayoutViewModel) {
    val filterText by viewModel.filterTextFlow.collectAsState(String.empty())
    val lazyItems = viewModel.poolsLayoutsFlow.collectAsLazyPagingItems()
    val onFilterChange = { newFilterText: String -> viewModel.applyFilter(newFilterText) }
    val pageSize = viewModel.pageSize
    PoolLayoutList(
        lazyPoolsLayouts = lazyItems,
        filterText = filterText,
        onFilterChange = onFilterChange,
        fakeItemCount = pageSize
    )
}

@Composable
fun PoolLayoutList(
    lazyPoolsLayouts: LazyPagingItems<PoolLayoutModel>,
    filterText: String,
    onFilterChange: (String) -> Unit,
    fakeItemCount: Int? = null
) {
    RefreshableLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        lazyItems = lazyPoolsLayouts,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        filterContent = {
            Filter(
                filterText = filterText,
                onFilterChange = onFilterChange
            )
        },
        onLoadingContent = fakeItemCount?.let { count ->
            {
                for (i in 1..count) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FakePoolLayoutItem(modifier = Modifier.fillMaxWidth())
                    Divider()
                }
            }
        }
    ) { poolLayout ->
        PoolLayoutItem(poolLayout = poolLayout, modifier = Modifier.fillMaxWidth())
        Divider()
    }
}

@Preview(showBackground = true)
@Composable
private fun PoolLayoutListPreview() {
    val lazyItems = poolsLayoutsModelsFlowForPreview().collectAsLazyPagingItems()
    PoolLayoutList(
        lazyPoolsLayouts = lazyItems,
        filterText = String.empty(),
        onFilterChange = {}
    )
}