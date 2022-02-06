package com.pipel.tyche.poolLayout.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.pipel.core.empty
import com.pipel.tyche.R
import com.pipel.tyche.poolLayout.view.PoolLayoutModel
import com.pipel.tyche.ui.column.RefreshableLazyColumn
import com.pipel.ui.SearcherTextField

@Composable
fun PoolLayoutView(viewModel: PoolLayoutViewModel = hiltViewModel()) {
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
    fakeItemCount: Int
) {
    RefreshableLazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        lazyItems = lazyPoolsLayouts,
        filterContent = {
            Column(Modifier.fillMaxWidth()) {
                SearcherTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = filterText,
                    onValueChange = onFilterChange
                ) {
                    Text(text = stringResource(id = R.string.searcher_label))
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider()
            }
        },
        onLoadingContent = {
            for (i in 1..fakeItemCount) {
                FakePoolLayoutItem()
            }
        },
        itemContent = { poolLayout ->
            PoolLayoutItem(poolLayout = poolLayout)
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PoolLayoutListPreview() {
    val lazyItems = poolLayoutFlowForPreview().collectAsLazyPagingItems()
    PoolLayoutList(
        lazyPoolsLayouts = lazyItems,
        filterText = String.empty(),
        onFilterChange = {},
        fakeItemCount = 10
    )
}