package com.pipel.tyche.poolGambler.ui

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
import com.pipel.tyche.poolGambler.view.PoolGamblerModel
import com.pipel.tyche.ui.Filter
import com.pipel.tyche.ui.column.RefreshableLazyColumn

@Composable
fun PoolGamblerView(viewModel: PoolGamblerViewModel) {
    val filterText by viewModel.filterTextFlow.collectAsState(String.empty())
    val lazyItems = viewModel.poolsGamblersFlow.collectAsLazyPagingItems()
    val onFilterChange = { newFilterText: String -> viewModel.applyFilter(newFilterText) }
    val pageSize = viewModel.pageSize
    PoolGamblerList(
        lazyPoolsGamblers = lazyItems,
        filterText = filterText,
        onFilterChange = onFilterChange,
        fakeItemCount = pageSize
    )
}

@Composable
fun PoolGamblerList(
    lazyPoolsGamblers: LazyPagingItems<PoolGamblerModel>,
    filterText: String,
    onFilterChange: (String) -> Unit,
    fakeItemCount: Int? = null
) {
    RefreshableLazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        lazyItems = lazyPoolsGamblers,
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
                    FakePoolGamblerItem(modifier = Modifier.fillMaxWidth())
                    Divider(modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    ) { poolGambler ->
        PoolGamblerItem(poolGambler = poolGambler, modifier = Modifier.fillMaxWidth())
        Divider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun PoolGamblerListPreview() {
    PoolGamblerList(
        lazyPoolsGamblers = poolsGamblersModelsFlowForPreview().collectAsLazyPagingItems(),
        filterText = String.empty(),
        onFilterChange = {}
    )
}