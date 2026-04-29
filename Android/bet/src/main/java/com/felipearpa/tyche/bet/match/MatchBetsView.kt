package com.felipearpa.tyche.bet.match

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.foundation.emptyString
import com.felipearpa.foundation.time.toShortDateTimeString
import com.felipearpa.tyche.bet.R
import com.felipearpa.tyche.bet.poolGamblerBetDummyModels
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import com.felipearpa.tyche.ui.R as SharedR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchBetsView(
    poolId: String,
    matchId: String,
    homeTeamName: String,
    awayTeamName: String,
    matchDateTime: LocalDateTime,
    homeTeamScore: Int?,
    awayTeamScore: Int?,
    onBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppTopBar(
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
            modifier = Modifier
                .padding(paddingValues = innerPadding)
                .fillMaxSize()
                .padding(all = LocalBoxSpacing.current.medium),
        ) {
            MatchHeader(
                homeTeamName = homeTeamName,
                awayTeamName = awayTeamName,
                matchDateTime = matchDateTime,
                homeTeamScore = homeTeamScore,
                awayTeamScore = awayTeamScore,
            )

            if (LocalInspectionMode.current) {
                val lazyItems =
                    MutableStateFlow(PagingData.from(poolGamblerBetDummyModels())).collectAsLazyPagingItems()
                MatchBetList(
                    lazyPoolGamblerBets = lazyItems,
                    placeholderCount = 50,
                    modifier = Modifier.fillMaxSize(),
                )
                return@Scaffold
            }

            MatchBetListView(
                viewModel = matchBetListViewModel(
                    poolId = poolId,
                    matchId = matchId,
                ),
            )
        }
    }
}

@Composable
private fun MatchHeader(
    homeTeamName: String,
    awayTeamName: String,
    matchDateTime: LocalDateTime,
    homeTeamScore: Int?,
    awayTeamScore: Int?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = homeTeamName,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = homeTeamScore.toString(),
                style = MaterialTheme.typography.titleLarge,
            )

            Text(text = "-")

            Text(
                text = awayTeamScore.toString(),
                style = MaterialTheme.typography.titleLarge,
            )

            Text(
                text = awayTeamName,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = matchDateTime.toShortDateTimeString(),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    onBack: () -> Unit,
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
        modifier = modifier,
        scrollBehavior = scrollBehavior,
    )
}

@Preview(showBackground = true)
@Composable
private fun MatBetsViewPreview() {
    MatchBetsView(
        poolId = "poolId",
        matchId = "matchId",
        homeTeamName = "Paris",
        awayTeamName = "Bayern Munchen",
        matchDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        homeTeamScore = 5,
        awayTeamScore = 4,
        onBack = {},
    )
}
