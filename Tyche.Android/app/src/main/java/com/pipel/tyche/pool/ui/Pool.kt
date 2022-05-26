package com.pipel.tyche.pool.ui

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
import com.pipel.tyche.pool.view.PoolModel
import com.pipel.tyche.ui.Filter
import com.pipel.tyche.ui.column.RefreshableLazyColumn

@Composable
fun PoolView(viewModel: PoolViewModel) {
    val filterText by viewModel.filterTextFlow.collectAsState(String.empty())
    val lazyItems = viewModel.poolsFlow.collectAsLazyPagingItems()
    val onFilterChange = { newFilterText: String -> viewModel.applyFilter(newFilterText) }
    val pageSize = viewModel.pageSize
    PoolList(
        lazyPools = lazyItems,
        filterText = filterText,
        onFilterChange = onFilterChange,
        fakeItemCount = pageSize
    )
}

@Composable
fun PoolList(
    lazyPools: LazyPagingItems<PoolModel>,
    filterText: String,
    onFilterChange: (String) -> Unit,
    fakeItemCount: Int
) {
    RefreshableLazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        lazyItems = lazyPools,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        filterContent = {
            Filter(
                filterText = filterText,
                onFilterChange = onFilterChange
            )
        },
        onLoadingContent = {
            for (i in 1..fakeItemCount) {
                Spacer(modifier = Modifier.height(8.dp))
                FakePoolItem(modifier = Modifier.fillMaxWidth())
                Divider()
            }
        }
    ) { pool ->
        PoolItem(pool = pool, modifier = Modifier.fillMaxWidth())
        Divider()
    }
}

@Preview(showBackground = true)
@Composable
private fun PoolListPreview() {
    PoolList(
        lazyPools = poolsModelsFlowForPreview().collectAsLazyPagingItems(),
        filterText = String.empty(),
        onFilterChange = {},
        fakeItemCount = 10
    )
}