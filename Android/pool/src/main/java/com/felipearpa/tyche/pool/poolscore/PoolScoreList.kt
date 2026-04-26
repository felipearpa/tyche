package com.felipearpa.tyche.pool.poolscore

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.pool.creator.PoolFromLayoutCreatorItem
import com.felipearpa.tyche.pool.creator.PoolLayoutModel
import com.felipearpa.tyche.pool.creator.poolLayoutDummyModels
import com.felipearpa.tyche.pool.creator.poolLayoutFakeModel
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModels
import com.felipearpa.tyche.ui.exception.localizedOrDefault
import com.felipearpa.tyche.ui.lazy.Failure
import com.felipearpa.tyche.ui.lazy.RefreshableStatefulLazyColumn
import com.felipearpa.tyche.ui.shimmer
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun PoolScoreList(
    modifier: Modifier = Modifier,
    lazyPoolGamblerScores: LazyPagingItems<PoolGamblerScoreModel>,
    lazyPoolLayouts: LazyPagingItems<PoolLayoutModel>,
    lazyListState: LazyListState = rememberLazyListState(),
    onPoolOpen: (poolId: String, gamblerId: String) -> Unit,
    onPoolJoin: (poolId: String) -> Unit,
    onPoolLayoutSelect: (PoolLayoutModel) -> Unit,
    onSeeAllTemplates: () -> Unit,
    fakeItemCount: Int = 50,
) {
    RefreshableStatefulLazyColumn(
        modifier = modifier,
        lazyPagingItems = lazyPoolGamblerScores,
        lazyListState = lazyListState,
        loadingContent = { poolScorePlaceholderList(count = fakeItemCount) },
        loadingContentOnConcatenate = { poolScorePlaceholderItem() },
        emptyContent = {
            poolScoreEmptyList(
                lazyPoolLayouts = lazyPoolLayouts,
                onPoolLayoutSelect = onPoolLayoutSelect,
                onSeeAllTemplates = onSeeAllTemplates,
            )
        },
        errorContent = { exception -> poolScoreErrorList(exception) },
    ) {
        items(
            count = lazyPoolGamblerScores.itemCount,
            key = lazyPoolGamblerScores.itemKey { poolGamblerScore ->
                Pair(
                    poolGamblerScore.poolId,
                    poolGamblerScore.gamblerId,
                )
            },
            contentType = lazyPoolGamblerScores.itemContentType { "PoolScore" },
        ) { index ->
            val poolGamblerScore = lazyPoolGamblerScores[index] ?: return@items

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onPoolOpen(poolGamblerScore.poolId, poolGamblerScore.gamblerId)
                    },
            ) {
                PoolScoreItem(
                    poolGamblerScore = poolGamblerScore,
                    onJoin = { onPoolJoin(poolGamblerScore.poolId) },
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider()
            }
        }
    }
}

private fun LazyListScope.poolScoreEmptyList(
    lazyPoolLayouts: LazyPagingItems<PoolLayoutModel>,
    onPoolLayoutSelect: (PoolLayoutModel) -> Unit,
    onSeeAllTemplates: () -> Unit,
) {
    item {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = emptyStateHorizontalPadding,
                    vertical = emptyStateHeroVerticalPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.people_playing),
                contentDescription = null,
                modifier = Modifier
                    .width(iconSize)
                    .aspectRatio(1536f / 1024f),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(id = R.string.pool_score_empty_list_title),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall,
                )

                Text(
                    text = stringResource(id = R.string.pool_score_empty_list_subtitle),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    item {
        Text(
            text = stringResource(id = R.string.pool_score_empty_list_templates_section),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = emptyStateHorizontalPadding,
                    end = emptyStateHorizontalPadding,
                    bottom = LocalBoxSpacing.current.medium,
                ),
        )
    }

    when (val refreshState = lazyPoolLayouts.loadState.refresh) {
        is LoadState.Loading -> poolLayoutPlaceholderList(count = popularTemplatesCount)

        is LoadState.Error -> item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = emptyStateHorizontalPadding,
                        vertical = LocalBoxSpacing.current.medium,
                    ),
            ) {
                Failure(
                    localizedException = refreshState.error.localizedOrDefault(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        else -> {
            val visibleCount = lazyPoolLayouts.itemCount.coerceAtMost(popularTemplatesCount)
            items(
                count = visibleCount,
                key = lazyPoolLayouts.itemKey { poolLayout -> poolLayout.id },
                contentType = lazyPoolLayouts.itemContentType { "PoolLayout" },
            ) { index ->
                lazyPoolLayouts[index]?.let { poolLayout ->
                    PoolFromLayoutCreatorItem(
                        poolLayout = poolLayout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = emptyStateHorizontalPadding,
                                vertical = LocalBoxSpacing.current.small,
                            )
                            .clickable { onPoolLayoutSelect(poolLayout) },
                    )
                }
            }

            if (visibleCount > 0) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = emptyStateHorizontalPadding,
                                end = emptyStateHorizontalPadding,
                                top = LocalBoxSpacing.current.medium,
                                bottom = emptyStateHeroVerticalPadding,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        TextButton(onClick = onSeeAllTemplates) {
                            Text(text = stringResource(id = R.string.see_all_templates_action))
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.poolLayoutPlaceholderList(count: Int) {
    repeat(count) {
        item {
            PoolFromLayoutCreatorItem(
                poolLayout = poolLayoutFakeModel(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = emptyStateHorizontalPadding,
                        vertical = LocalBoxSpacing.current.small,
                    ),
                shimmerModifier = Modifier.shimmer(),
            )
        }
    }
}

private fun LazyListScope.poolScoreErrorList(exception: Throwable) {
    item {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillParentMaxSize()
                .padding(all = LocalBoxSpacing.current.medium),
        ) {
            Failure(
                localizedException = exception.localizedOrDefault(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun LazyListScope.poolScorePlaceholderList(count: Int) {
    repeat(count) {
        poolScorePlaceholderItem()
    }
}

private fun LazyListScope.poolScorePlaceholderItem() {
    item {
        PoolScorePlaceholderItem(modifier = Modifier.fillMaxWidth())
        HorizontalDivider()
    }
}

private val iconSize = 384.dp
private val emptyStateHorizontalPadding = 16.dp
private val emptyStateHeroVerticalPadding = 32.dp
private const val popularTemplatesCount = 3

@Preview(showBackground = true)
@Composable
private fun PoolScoreListPreview() {
    val items =
        MutableStateFlow(PagingData.from(poolGamblerScoreDummyModels())).collectAsLazyPagingItems()
    val emptyLayouts =
        MutableStateFlow(PagingData.empty<PoolLayoutModel>()).collectAsLazyPagingItems()
    PoolScoreList(
        lazyPoolGamblerScores = items,
        lazyPoolLayouts = emptyLayouts,
        onPoolOpen = { _, _ -> },
        onPoolJoin = {},
        onPoolLayoutSelect = {},
        onSeeAllTemplates = {},
        modifier = Modifier.fillMaxSize(),
    )
}

@PreviewLightDark
@Composable
private fun PoolScoreEmptyListPreview() {
    val layouts =
        MutableStateFlow(PagingData.from(poolLayoutDummyModels())).collectAsLazyPagingItems()
    TycheTheme {
        Surface {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = LocalBoxSpacing.current.medium),
            ) {
                poolScoreEmptyList(
                    lazyPoolLayouts = layouts,
                    onPoolLayoutSelect = {},
                    onSeeAllTemplates = {},
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PoolScorePlaceholderListPreview() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = LocalBoxSpacing.current.medium),
    ) {
        poolScorePlaceholderList(count = 50)
    }
}
