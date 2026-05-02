package com.felipearpa.tyche.bet.match

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.R
import com.felipearpa.tyche.bet.isPending
import com.felipearpa.tyche.bet.poolGamblerBetDummyModel
import com.felipearpa.tyche.bet.poolGamblerBetDummyModels
import com.felipearpa.tyche.ui.exception.localizedOrDefault
import com.felipearpa.tyche.ui.lazy.Failure
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.ui.state.LoadableViewState
import kotlinx.coroutines.flow.MutableStateFlow
import com.felipearpa.tyche.ui.R as SharedR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchBetListView(
    viewModel: MatchBetListViewModel,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onGamblerOpen: ((poolId: String, gamblerId: String, gamblerUsername: String) -> Unit)? = null,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val poolGamblerBetState by viewModel.poolGamblerBetState.collectAsState()
    val lazyPoolGamblerBets = viewModel.poolGamblerBets.collectAsLazyPagingItems()

    MatchBetListView(
        poolGamblerBetState = poolGamblerBetState,
        lazyPoolGamblerBets = lazyPoolGamblerBets,
        pageSize = viewModel.pageSize,
        onBack = onBack,
        onHome = onHome,
        onRetry = viewModel::loadPoolGamblerBet,
        onGamblerOpen = onGamblerOpen,
        scrollBehavior = scrollBehavior,
    )

    LaunchedEffect(Unit) {
        viewModel.loadPoolGamblerBet()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchBetListView(
    poolGamblerBetState: LoadableViewState<PoolGamblerBetModel>,
    lazyPoolGamblerBets: LazyPagingItems<PoolGamblerBetModel>,
    pageSize: Int,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onRetry: () -> Unit,
    onGamblerOpen: ((poolId: String, gamblerId: String, gamblerUsername: String) -> Unit)?,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppTopBar(
                onBack = onBack,
                onHome = onHome,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        MatchBetListContent(
            poolGamblerBetState = poolGamblerBetState,
            lazyPoolGamblerBets = lazyPoolGamblerBets,
            pageSize = pageSize,
            onRetry = onRetry,
            onGamblerOpen = onGamblerOpen,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(all = LocalBoxSpacing.current.medium),
        )
    }
}

@Composable
private fun MatchBetListContent(
    poolGamblerBetState: LoadableViewState<PoolGamblerBetModel>,
    lazyPoolGamblerBets: LazyPagingItems<PoolGamblerBetModel>,
    pageSize: Int,
    onRetry: () -> Unit,
    onGamblerOpen: ((poolId: String, gamblerId: String, gamblerUsername: String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    when (poolGamblerBetState) {
        is LoadableViewState.Initial, LoadableViewState.Loading -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
                modifier = modifier,
            ) {
                MatchHeaderPlaceholderItem()

                MatchBetList(
                    lazyPoolGamblerBets = lazyPoolGamblerBets,
                    placeholderCount = pageSize,
                    onGamblerOpen = onGamblerOpen,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        is LoadableViewState.Failure -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.large),
                ) {
                    Failure(
                        localizedException = poolGamblerBetState.exception.localizedOrDefault(),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(id = SharedR.string.retry_action))
                    }
                }
            }
        }

        is LoadableViewState.Success -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
                modifier = modifier,
            ) {
                MatchHeader(bet = poolGamblerBetState.value)

                if (poolGamblerBetState.value.isPending) {
                    PredictionsOpenContent(modifier = Modifier.fillMaxSize())
                } else {
                    MatchBetList(
                        lazyPoolGamblerBets = lazyPoolGamblerBets,
                        placeholderCount = pageSize,
                        onGamblerOpen = onGamblerOpen,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PredictionsOpenContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(all = LocalBoxSpacing.current.large),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.large),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(id = SharedR.drawable.lock),
                contentDescription = emptyString(),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(id = R.string.predictions_open_title),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = stringResource(id = R.string.predictions_open_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    onBack: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.match_bets_view_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = SharedR.drawable.arrow_back),
                    contentDescription = emptyString(),
                )
            }
        },
        actions = {
            IconButton(onClick = onHome) {
                Icon(
                    painter = painterResource(id = SharedR.drawable.home),
                    contentDescription = emptyString(),
                )
            }
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
    )
}

private class MatchBetListViewStateProvider : PreviewParameterProvider<LoadableViewState<PoolGamblerBetModel>> {
    override val values = sequenceOf(
        LoadableViewState.Loading,
        LoadableViewState.Failure(Exception("Error loading bets")),
        LoadableViewState.Success(poolGamblerBetDummyModel()),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun MatchBetListViewPreview(
    @PreviewParameter(MatchBetListViewStateProvider::class) state: LoadableViewState<PoolGamblerBetModel>,
) {
    val lazyItems =
        MutableStateFlow(PagingData.from(poolGamblerBetDummyModels())).collectAsLazyPagingItems()

    TycheTheme {
        Surface {
            MatchBetListView(
                poolGamblerBetState = state,
                lazyPoolGamblerBets = lazyItems,
                pageSize = 50,
                onBack = {},
                onHome = {},
                onRetry = {},
                onGamblerOpen = { _, _, _ -> },
                scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState()),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun MatchBetListViewPendingPreview() {
    val lazyItems =
        MutableStateFlow(PagingData.from(poolGamblerBetDummyModels())).collectAsLazyPagingItems()

    TycheTheme {
        Surface {
            MatchBetListView(
                poolGamblerBetState = LoadableViewState.Success(
                    poolGamblerBetDummyModel().copy(isLocked = false, isComputed = false),
                ),
                lazyPoolGamblerBets = lazyItems,
                pageSize = 50,
                onBack = {},
                onHome = {},
                onRetry = {},
                onGamblerOpen = { _, _, _ -> },
                scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState()),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun MatchBetListViewLivePreview() {
    val lazyItems =
        MutableStateFlow(PagingData.from(poolGamblerBetDummyModels())).collectAsLazyPagingItems()

    TycheTheme {
        Surface {
            MatchBetListView(
                poolGamblerBetState = LoadableViewState.Success(
                    poolGamblerBetDummyModel().copy(isLocked = true, isComputed = false),
                ),
                lazyPoolGamblerBets = lazyItems,
                pageSize = 50,
                onBack = {},
                onHome = {},
                onRetry = {},
                onGamblerOpen = { _, _, _ -> },
                scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState()),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun MatchBetListViewComputedPreview() {
    val lazyItems =
        MutableStateFlow(PagingData.from(poolGamblerBetDummyModels())).collectAsLazyPagingItems()

    TycheTheme {
        Surface {
            MatchBetListView(
                poolGamblerBetState = LoadableViewState.Success(
                    poolGamblerBetDummyModel().copy(isLocked = true, isComputed = true),
                ),
                lazyPoolGamblerBets = lazyItems,
                pageSize = 50,
                onBack = {},
                onHome = {},
                onRetry = {},
                onGamblerOpen = { _, _, _ -> },
                scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState()),
            )
        }
    }
}
