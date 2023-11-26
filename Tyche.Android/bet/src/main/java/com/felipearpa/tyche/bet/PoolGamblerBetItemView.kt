package com.felipearpa.tyche.bet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.ui.LocalizedException
import com.felipearpa.tyche.ui.onFailure
import com.felipearpa.tyche.ui.onLoading
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Composable
fun PoolGamblerBetItemView(
    viewModel: PoolGamblerBetItemViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PoolGamblerBetItem(
            poolGamblerBet = viewModel.poolGamblerBet,
            bet = viewModel::bet,
            modifier = modifier
        )

        state.onFailure { exception ->
            if (exception !is BetLocalizedException.Forbidden) {
                Text(
                    text = (exception as LocalizedException).failureReason ?: emptyString(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(onClick = {
                        viewModel.retryBet()
                        coroutineScope.launch {
                            focusManager.clearFocus()
                        }
                    }) {
                        Text(text = stringResource(id = R.string.retry_action))
                    }

                    Button(
                        onClick = {
                            viewModel.restoreBet()
                            coroutineScope.launch {
                                focusManager.clearFocus()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(
                            text = stringResource(id = R.string.cancel_action),
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
        }.onLoading {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun PoolGamblerBetItem(
    poolGamblerBet: PoolGamblerBetModel,
    bet: (TeamScore<Int>) -> Unit,
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier
) {
    var homeTeamBet by remember(poolGamblerBet.betScore?.homeTeamValue) {
        mutableStateOf(
            (poolGamblerBet.betScore?.homeTeamValue ?: emptyString()).toString()
        )
    }
    var awayTeamBet by remember(poolGamblerBet.betScore?.awayTeamValue) {
        mutableStateOf(
            (poolGamblerBet.betScore?.awayTeamValue ?: emptyString()).toString()
        )
    }

    Column(modifier = modifier) {
        ConstraintLayout(modifier = Modifier.fillMaxWidth()) {
            val (teamView, betView) = createRefs()

            Column(modifier = Modifier.constrainAs(teamView) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                width = Dimension.fillToConstraints
            }) {
                Text(text = poolGamblerBet.homeTeamName, modifier = shimmerModifier)
                Text(text = poolGamblerBet.awayTeamName, modifier = shimmerModifier)
            }

            Box(modifier = Modifier.constrainAs(betView) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end)
            }) {
                if (poolGamblerBet.isLocked()) {
                    NonEditableBetScore(
                        homeTeamBet = homeTeamBet,
                        awayTeamBet = awayTeamBet,
                        shimmerModifier = shimmerModifier
                    )
                } else {
                    EditableBetScore(
                        homeTeamBet = homeTeamBet,
                        awayTeamBet = awayTeamBet,
                        onBetValueChange = { newHomeTeamBet, newAwayTeamBet ->
                            homeTeamBet = newHomeTeamBet
                            awayTeamBet = newAwayTeamBet
                        },
                        onBetValueAsyncChange = { newHomeTeamBet, newAwayTeamBet ->
                            bet(
                                TeamScore(
                                    homeTeamValue = newHomeTeamBet.toInt(),
                                    awayTeamValue = newAwayTeamBet.toInt()
                                )
                            )
                        },
                        shimmerModifier = shimmerModifier
                    )
                }
            }
        }
    }
}

@Composable
fun EditableBetScore(
    homeTeamBet: String,
    awayTeamBet: String,
    onBetValueChange: (String, String) -> Unit,
    onBetValueAsyncChange: (String, String) -> Unit,
    shimmerModifier: Modifier = Modifier
) {
    Column {
        BetTextField(
            value = homeTeamBet,
            onValueChange = { newBet -> onBetValueChange(newBet, awayTeamBet) },
            onDelayedValueChange = { newBet -> onBetValueAsyncChange(newBet, awayTeamBet) },
            modifier = Modifier
                .width(3 * with(LocalDensity.current) { LocalTextStyle.current.fontSize.toDp() })
                .then(shimmerModifier)
        )
        BetTextField(
            value = awayTeamBet,
            onValueChange = { newBet -> onBetValueChange(homeTeamBet, newBet) },
            onDelayedValueChange = { newBet -> onBetValueAsyncChange(homeTeamBet, newBet) },
            modifier = Modifier
                .width(3 * with(LocalDensity.current) { LocalTextStyle.current.fontSize.toDp() })
                .then(shimmerModifier)
        )
    }
}

@Composable
fun NonEditableBetScore(
    homeTeamBet: String,
    awayTeamBet: String,
    shimmerModifier: Modifier = Modifier
) {
    Column {
        Text(
            text = homeTeamBet,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(3 * with(LocalDensity.current) { LocalTextStyle.current.fontSize.toDp() })
                .then(shimmerModifier)
        )
        Text(
            text = awayTeamBet,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(3 * with(LocalDensity.current) { LocalTextStyle.current.fontSize.toDp() })
                .then(shimmerModifier)
        )
    }
}

class PoolGamblerBetItemParameterProvider : PreviewParameterProvider<PoolGamblerBetModel> {
    override val values = sequenceOf(
        PoolGamblerBetModel(
            poolId = "X".repeat(15),
            gamblerId = "X".repeat(15),
            matchId = "X".repeat(15),
            homeTeamId = "X".repeat(15),
            homeTeamName = "Colombia",
            matchScore = TeamScore(2, 1),
            betScore = TeamScore(2, 1),
            awayTeamId = "X".repeat(15),
            awayTeamName = "Argentina",
            score = 10,
            matchDateTime = LocalDateTime.now().minusDays(1)
        ),
        PoolGamblerBetModel(
            poolId = "X".repeat(15),
            gamblerId = "X".repeat(15),
            matchId = "X".repeat(15),
            homeTeamId = "X".repeat(15),
            homeTeamName = "Colombia",
            matchScore = null,
            betScore = TeamScore(2, 1),
            awayTeamId = "X".repeat(15),
            awayTeamName = "Argentina",
            score = null,
            matchDateTime = LocalDateTime.now().plusDays(1)
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun PoolGamblerBetItemPreview(
    @PreviewParameter(PoolGamblerBetItemParameterProvider::class) poolGamblerBet: PoolGamblerBetModel
) {
    MaterialTheme {
        PoolGamblerBetItem(
            poolGamblerBet = poolGamblerBet,
            bet = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}