package com.felipearpa.tyche.pool.creator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun PoolFromLayoutCreatorView(viewModel: PoolFromLayoutCreatorViewModel) {
    val viewModelState = viewModel.poolLayouts.collectAsLazyPagingItems()
    val pageSize = viewModel.pageSize

    PoolFromLayoutCreatorView(lazyItems = viewModelState, pageSize = pageSize)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoolFromLayoutCreatorView(
    lazyItems: LazyPagingItems<PoolLayoutModel>,
    pageSize: Int,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBar(
                title = { Text(text = stringResource(id = R.string.pool_from_layout_creator_title)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        StepOne(
            lazyItems = lazyItems,
            pageSize = pageSize,
            modifier = Modifier
                .padding(paddingValues = paddingValues)
                .fillMaxSize()
                .padding(all = LocalBoxSpacing.current.medium),
        )
    }
}

@Composable
private fun StepOne(
    modifier: Modifier = Modifier,
    lazyItems: LazyPagingItems<PoolLayoutModel>,
    pageSize: Int,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
    ) {
        Text(
            text = stringResource(id = R.string.select_pool_layout_text),
            style = MaterialTheme.typography.titleMedium,
        )

        PoolFromLayoutCreatorList(
            poolLayouts = lazyItems,
            fakeItemCount = pageSize,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    title: @Composable () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    TopAppBar(
        title = title,
        scrollBehavior = scrollBehavior,
    )
}

@PreviewLightDark
@Composable
private fun PoolFromLayoutCreatorViewPreview() {
    val items =
        MutableStateFlow(PagingData.from(poolLayoutDummyModels())).collectAsLazyPagingItems()

    TycheTheme {
        Surface {
            PoolFromLayoutCreatorView(
                lazyItems = items,
                pageSize = 50,
            )
        }
    }
}
