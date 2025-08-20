package com.felipearpa.tyche.pool.poolscore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModels
import com.felipearpa.tyche.ui.lazy.Failure
import com.felipearpa.tyche.ui.lazy.RefreshableStatefulLazyColumn
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun PoolScoreList(
    modifier: Modifier = Modifier,
    lazyPoolGamblerScores: LazyPagingItems<PoolGamblerScoreModel>,
    lazyListState: LazyListState = rememberLazyListState(),
    onPoolOpen: (poolId: String, gamblerId: String) -> Unit,
    onPoolJoin: (poolId: String) -> Unit,
    onPoolCreate: () -> Unit,
    fakeItemCount: Int = 50,
) {
    RefreshableStatefulLazyColumn(
        modifier = modifier,
        lazyItems = lazyPoolGamblerScores,
        state = lazyListState,
        loadingContent = { poolScorePlaceholderList(count = fakeItemCount) },
        loadingContentOnConcatenate = { poolScorePlaceholderItem() },
        emptyContent = { poolScoreEmptyList(onPoolCreate = onPoolCreate) },
        errorContent = { poolScoreErrorList() },
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

private fun LazyListScope.poolScoreEmptyList(onPoolCreate: () -> Unit) {
    item {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillParentMaxSize()
                .padding(all = LocalBoxSpacing.current.medium),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.emoji_people),
                    contentDescription = emptyString(),
                    modifier = Modifier.size(iconSize),
                )

                Text(
                    text = stringResource(id = R.string.pool_score_empty_list_title),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )

                Text(
                    text = stringResource(id = R.string.pool_score_empty_list_subtitle),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                )

                Spacer(modifier = Modifier.height(LocalBoxSpacing.current.medium))

                Button(
                    onClick = onPoolCreate,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(id = R.string.create_pool_action))
                }
            }
        }
    }
}

private fun LazyListScope.poolScoreErrorList() {
    item {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillParentMaxSize()
                .padding(all = LocalBoxSpacing.current.medium),
        ) {
            Failure(modifier = Modifier.fillMaxWidth())
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

private val iconSize = 64.dp

@Preview(showBackground = true)
@Composable
private fun PoolScoreListPreview() {
    val items =
        MutableStateFlow(PagingData.from(poolGamblerScoreDummyModels())).collectAsLazyPagingItems()
    PoolScoreList(
        lazyPoolGamblerScores = items,
        onPoolOpen = { _, _ -> },
        onPoolJoin = {},
        onPoolCreate = {},
        modifier = Modifier.fillMaxSize(),
    )
}

@Preview(showBackground = true)
@Composable
private fun PoolScoreEmptyListPreview() {
    val items =
        MutableStateFlow(PagingData.from(emptyList<PoolGamblerScoreModel>())).collectAsLazyPagingItems()
    PoolScoreList(
        lazyPoolGamblerScores = items,
        onPoolOpen = { _, _ -> },
        onPoolJoin = {},
        onPoolCreate = {},
        modifier = Modifier.fillMaxSize(),
    )
}

@Preview(showBackground = true)
@Composable
private fun PoolScoreFakeListPreview() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = LocalBoxSpacing.current.medium),
    ) {
        poolScorePlaceholderList(count = 50)
    }
}
