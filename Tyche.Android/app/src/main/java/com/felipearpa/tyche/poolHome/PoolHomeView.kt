package com.felipearpa.tyche.poolHome

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.felipearpa.tyche.R
import com.felipearpa.tyche.bet.poolGamblerBetListViewModel
import com.felipearpa.tyche.bet.ui.PoolGamblerBetListView
import com.felipearpa.tyche.bet.ui.PoolGamblerBetListViewModel
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.pool.gamblerScoreListViewModel
import com.felipearpa.tyche.pool.ui.gamblerScore.GamblerScoreListView
import com.felipearpa.tyche.pool.ui.gamblerScore.GamblerScoreListViewModel
import com.felipearpa.tyche.ui.LocalizedException
import com.felipearpa.tyche.ui.Message
import com.felipearpa.tyche.ui.ViewState
import com.felipearpa.tyche.ui.onLoading
import com.felipearpa.tyche.ui.onSuccess
import com.felipearpa.tyche.ui.shimmer

enum class Tab {
    GAMBLER_SCORE,
    BET_EDITOR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
    poolScoreListRequested: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable { poolScoreListRequested() }
                    .then(shimmerModifier)
            )
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoolHomeView(viewModel: PoolHomeViewModel, onPoolScoreListRequested: () -> Unit) {
    val state by viewModel.state.collectAsState()

    when (state) {
        is ViewState.Failure -> {
            val exception = (state as ViewState.Failure).invoke()
            Message(
                iconResourceId = R.drawable.ic_sentiment_sad,
                message = (exception as LocalizedException).failureReason ?: emptyString()
            )
        }

        else -> Scaffold(
            topBar = {
                state.onSuccess { pool ->
                    AppTopBar(
                        title = pool.poolName,
                        poolScoreListRequested = onPoolScoreListRequested
                    )
                }.onLoading {
                    AppTopBar(
                        title = "XXXXXXXXXXXXXXXXXXXX",
                        poolScoreListRequested = {},
                        shimmerModifier = Modifier.shimmer()
                    )
                }
            }
        ) { paddingValues ->
            Content(
                gamblerScoreListViewModel = gamblerScoreListViewModel(
                    poolId = viewModel.poolId,
                    gamblerId = viewModel.gamblerId
                ),
                poolGamblerBetListViewModel = poolGamblerBetListViewModel(
                    poolId = viewModel.poolId,
                    gamblerId = viewModel.gamblerId
                ),
                modifier = Modifier
                    .padding(paddingValues = paddingValues)
                    .fillMaxSize()
            )
        }
    }
}

@Composable
private fun Content(
    gamblerScoreListViewModel: GamblerScoreListViewModel,
    poolGamblerBetListViewModel: PoolGamblerBetListViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(Tab.GAMBLER_SCORE) }

    ConstraintLayout(modifier = modifier) {
        val (itemView, tabView) = createRefs()

        Box(modifier = Modifier.constrainAs(itemView) {
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            top.linkTo(parent.top)
            height = Dimension.matchParent
        }) {
            when (selectedTabIndex) {
                Tab.GAMBLER_SCORE -> GamblerScoreListView(viewModel = gamblerScoreListViewModel)

                Tab.BET_EDITOR -> PoolGamblerBetListView(viewModel = poolGamblerBetListViewModel)
            }
        }

        TabRow(
            selectedTabIndex = selectedTabIndex.ordinal,
            modifier = Modifier.constrainAs(tabView) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(parent.bottom)
            }
        ) {
            Tab(
                selected = selectedTabIndex == Tab.GAMBLER_SCORE,
                onClick = { selectedTabIndex = Tab.GAMBLER_SCORE }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sport_score),
                        contentDescription = emptyString(),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.score_tab),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Tab(
                selected = selectedTabIndex == Tab.BET_EDITOR,
                onClick = { selectedTabIndex = Tab.BET_EDITOR }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_money),
                        contentDescription = emptyString(),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.bet_tab),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppTopBarPreview() {
    AppTopBar(title = "American Cup Tyche 2023", poolScoreListRequested = {})
}