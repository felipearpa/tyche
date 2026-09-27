package com.felipearpa.tyche.pool.gamblerscore

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.plus
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModels
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun GamblerScoreListView(
    viewModel: GamblerScoreListViewModel,
    signedInGamblerId: String = viewModel.gamblerId,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onGamblerOpen: ((poolId: String, gamblerId: String, gamblerUsername: String) -> Unit)? = null,
) {
    val lazyItems = viewModel.poolGamblerScores.collectAsLazyPagingItems()
    val pageSize = viewModel.pageSize
    GamblerScoreListView(
        lazyPoolGamblerScores = lazyItems,
        gamblerId = signedInGamblerId,
        placeholderItemCount = pageSize,
        onGamblerOpen = onGamblerOpen,
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun GamblerScoreListView(
    lazyPoolGamblerScores: LazyPagingItems<PoolGamblerScoreModel>,
    gamblerId: String,
    placeholderItemCount: Int,
    onGamblerOpen: ((poolId: String, gamblerId: String, gamblerUsername: String) -> Unit)?,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    // The list scrolls under the screen's app bars; its content padding keeps the first and
    // last rows clear of them.
    GamblerScoreList(
        lazyPoolGamblerScores = lazyPoolGamblerScores,
        loggedInGamblerId = gamblerId,
        placeholderCount = placeholderItemCount,
        onGamblerOpen = onGamblerOpen,
        contentPadding = contentPadding + PaddingValues(vertical = LocalBoxSpacing.current.medium),
        modifier = modifier.consumeWindowInsets(contentPadding),
    )
}

@PreviewLightDark
@Composable
fun GamblerScoreListViewPreview(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    val items = MutableStateFlow(PagingData.from(poolGamblerScoreDummyModels())).collectAsLazyPagingItems()

    TycheTheme {
        Surface {
            GamblerScoreListView(
                lazyPoolGamblerScores = items,
                gamblerId = "gambler001",
                placeholderItemCount = 50,
                onGamblerOpen = { _, _, _ -> },
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
