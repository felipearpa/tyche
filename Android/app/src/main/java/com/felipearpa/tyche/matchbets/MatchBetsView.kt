package com.felipearpa.tyche.matchbets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.felipearpa.foundation.emptyString
import com.felipearpa.foundation.time.toShortDateTimeString
import com.felipearpa.tyche.bet.match.MatchBetListView
import com.felipearpa.tyche.bet.match.matchBetListViewModel
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import kotlinx.datetime.LocalDateTime
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
            modifier = Modifier
                .padding(paddingValues = innerPadding)
                .fillMaxSize(),
        ) {
            MatchHeader(
                homeTeamName = homeTeamName,
                awayTeamName = awayTeamName,
                matchDateTime = matchDateTime,
                homeTeamScore = homeTeamScore,
                awayTeamScore = awayTeamScore,
            )

            HorizontalDivider()

            Box(modifier = Modifier.fillMaxSize()) {
                MatchBetListView(
                    viewModel = matchBetListViewModel(
                        poolId = poolId,
                        matchId = matchId,
                    ),
                )
            }
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
    val hasScore = homeTeamScore != null && awayTeamScore != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = LocalBoxSpacing.current.medium),
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = homeTeamName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            if (hasScore) {
                Text(
                    text = "$homeTeamScore - $awayTeamScore",
                    style = MaterialTheme.typography.titleLarge,
                )
            } else {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = awayTeamName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier.weight(1f),
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
        title = {},
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
