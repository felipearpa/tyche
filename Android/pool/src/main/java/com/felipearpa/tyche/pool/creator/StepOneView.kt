package com.felipearpa.tyche.pool.creator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
internal fun StepOneView(
    viewModel: StepOneViewModel,
    createPoolModel: CreatePoolModel,
    onNextClick: (createPoolModel: CreatePoolModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyPoolLayouts = viewModel.poolLayouts.collectAsLazyPagingItems()
    StepOne(
        lazyItems = lazyPoolLayouts,
        pageSize = 3,
        createPoolModel = createPoolModel,
        onNextClick = onNextClick,
        modifier = modifier,
    )
}

@Composable
private fun StepOne(
    lazyItems: LazyPagingItems<PoolLayoutModel>,
    pageSize: Int,
    createPoolModel: CreatePoolModel,
    onNextClick: (createPoolModel: CreatePoolModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedPoolLayout by remember { mutableStateOf<PoolLayoutModel?>(null) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
    ) {

        Text(
            text = stringResource(id = R.string.choose_pool_layout_text),
            style = MaterialTheme.typography.titleMedium,
        )

        PoolFromLayoutCreatorList(
            poolLayouts = lazyItems,
            fakeItemCount = pageSize,
            modifier = Modifier.fillMaxWidth(),
            selectedPoolLayout = selectedPoolLayout,
            onPoolLayoutChange = { newPoolLayout -> selectedPoolLayout = newPoolLayout },
        )
    }

    LaunchedEffect(selectedPoolLayout) {
        selectedPoolLayout?.let {
            onNextClick(createPoolModel.copy(poolLayoutId = it.id, poolName = it.name))
        }
    }
}

@PreviewLightDark
@Composable
private fun StepOnePreview() {
    val items =
        MutableStateFlow(PagingData.from(poolLayoutDummyModels())).collectAsLazyPagingItems()

    TycheTheme {
        Surface {
            StepOne(
                lazyItems = items,
                pageSize = 5,
                createPoolModel = emptyCreatePoolModel(),
                onNextClick = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
