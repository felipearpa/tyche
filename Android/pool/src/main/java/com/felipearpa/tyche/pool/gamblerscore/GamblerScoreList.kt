package com.felipearpa.tyche.pool.gamblerscore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModels
import com.felipearpa.tyche.ui.lazy.RefreshableLazyPagingColumn
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun GamblerScoreList(
    lazyPoolGamblerScores: LazyPagingItems<PoolGamblerScoreModel>,
    loggedInGamblerId: String,
    modifier: Modifier = Modifier,
    placeholderCount: Int = 0,
    onGamblerOpen: ((poolId: String, gamblerId: String, gamblerUsername: String) -> Unit)? = null,
) {
    RefreshableLazyPagingColumn(
        modifier = modifier,
        lazyPagingItems = lazyPoolGamblerScores,
        loadingContent = { gamblerScorePlaceholderList(count = placeholderCount) },
        appendLoadingContent = { gamblerScorePlaceholderItemRow() },
    ) {
        items(
            count = lazyPoolGamblerScores.itemCount,
            key = lazyPoolGamblerScores.itemKey(
                key = { poolGamblerScore ->
                    Pair(
                        poolGamblerScore.poolId,
                        poolGamblerScore.gamblerId,
                    )
                },
            ),
            contentType = lazyPoolGamblerScores.itemContentType { "GamblerScore" },
        ) { index ->
            val item = lazyPoolGamblerScores[index] ?: return@items
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { base ->
                        if (onGamblerOpen != null) {
                            base.clickable {
                                onGamblerOpen(
                                    item.poolId,
                                    item.gamblerId,
                                    item.gamblerUsername,
                                )
                            }
                        } else {
                            base
                        }
                    }
                    .padding(horizontal = LocalBoxSpacing.current.medium),
            ) {
                GamblerScoreItem(
                    poolGamblerScore = item,
                    isCurrentUser = item.gamblerId == loggedInGamblerId,
                    modifier = Modifier.gamblerScoreItem(),
                )
                HorizontalDivider()
            }
        }
    }
}

private fun LazyListScope.gamblerScorePlaceholderList(count: Int) {
    repeat(count) {
        gamblerScorePlaceholderItemRow()
    }
}

private fun LazyListScope.gamblerScorePlaceholderItemRow() {
    item {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LocalBoxSpacing.current.medium),
        ) {
            GamblerScorePlaceholderItem(modifier = Modifier.gamblerScoreItem())
            HorizontalDivider()
        }
    }
}

@Composable
private fun Modifier.gamblerScoreItem() =
    fillMaxWidth().padding(all = LocalBoxSpacing.current.medium)

@PreviewLightDark
@Composable
private fun GamblerScoreListPreview() {
    val items =
        MutableStateFlow(PagingData.from(poolGamblerScoreDummyModels())).collectAsLazyPagingItems()

    TycheTheme {
        Surface {
            GamblerScoreList(
                lazyPoolGamblerScores = items,
                loggedInGamblerId = "gambler001",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun GamblerScoreFakeListPreview() {
    TycheTheme {
        Surface {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = LocalBoxSpacing.current.medium),
            ) {
                gamblerScorePlaceholderList(count = 50)
            }
        }
    }
}
