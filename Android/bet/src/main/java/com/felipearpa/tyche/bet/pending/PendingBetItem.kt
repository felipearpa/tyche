package com.felipearpa.tyche.bet.pending

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.foundation.time.toShortDateTimeString
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.poolGamblerBetDummyModel
import com.felipearpa.tyche.bet.poolGamblerBetFakeModel
import com.felipearpa.tyche.bet.scoreWidth
import com.felipearpa.tyche.ui.FlagImage
import com.felipearpa.tyche.ui.shimmer
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme

@Composable
fun PendingBetItem(
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
    poolGamblerBet: PoolGamblerBetModel,
    viewState: PendingBetItemViewState,
    onBetChanged: (PartialPoolGamblerBetModel) -> Unit = {},
) {
    val enterTransition = fadeIn() + expandVertically()
    val exitTransition = shrinkVertically() + fadeOut()

    Box {
        AnimatedVisibility(
            visible = viewState is PendingBetItemViewState.Visualization,
            enter = enterTransition,
            exit = exitTransition,
        ) {
            NonEditablePendingBetItem(
                modifier = modifier,
                shimmerModifier = shimmerModifier,
                poolGamblerBet = poolGamblerBet,
                partialPoolGamblerBet = viewState.value,
            )
        }

        AnimatedVisibility(
            visible = viewState is PendingBetItemViewState.Edition,
            enter = enterTransition,
            exit = exitTransition,
        ) {
            EditablePendingBetItem(
                modifier = modifier,
                shimmerModifier = shimmerModifier,
                poolGamblerBet = poolGamblerBet,
                partialPoolGamblerBet = viewState.value,
                onBetChanged = onBetChanged,
            )
        }
    }
}

@Composable
fun NonEditablePendingBetItem(
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
    poolGamblerBet: PoolGamblerBetModel,
    partialPoolGamblerBet: PartialPoolGamblerBetModel,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
            ) {
                FlagImage(
                    teamId = poolGamblerBet.homeTeamId,
                    modifier = Modifier
                        .size(flagSize)
                        .then(shimmerModifier),
                )
                Text(text = poolGamblerBet.homeTeamName, modifier = shimmerModifier)
            }
            Text(text = partialPoolGamblerBet.homeTeamBet, modifier = shimmerModifier)
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
            ) {
                FlagImage(
                    teamId = poolGamblerBet.awayTeamId,
                    modifier = Modifier
                        .size(flagSize)
                        .then(shimmerModifier),
                )
                Text(text = poolGamblerBet.awayTeamName, modifier = shimmerModifier)
            }
            Text(text = partialPoolGamblerBet.awayTeamBet, modifier = shimmerModifier)
        }

        Text(
            text = poolGamblerBet.matchDateTime.toShortDateTimeString(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(start = LocalBoxSpacing.current.large)
                .then(shimmerModifier),
        )
    }
}

@Composable
fun EditablePendingBetItem(
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
    poolGamblerBet: PoolGamblerBetModel,
    partialPoolGamblerBet: PartialPoolGamblerBetModel,
    onBetChanged: (PartialPoolGamblerBetModel) -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlagImage(
                    teamId = poolGamblerBet.homeTeamId,
                    modifier = Modifier
                        .size(flagSize)
                        .then(shimmerModifier),
                )
                Text(text = poolGamblerBet.homeTeamName, modifier = shimmerModifier)
            }
            BetTextField(
                value = partialPoolGamblerBet.homeTeamBet,
                onValueChange = { newHomeTeamBet ->
                    onBetChanged(partialPoolGamblerBet.copy(homeTeamBet = newHomeTeamBet))
                },
                modifier = Modifier
                    .scoreWidth()
                    .then(shimmerModifier),
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlagImage(
                    teamId = poolGamblerBet.awayTeamId,
                    modifier = Modifier
                        .size(flagSize)
                        .then(shimmerModifier),
                )
                Text(text = poolGamblerBet.awayTeamName, modifier = shimmerModifier)
            }
            BetTextField(
                value = partialPoolGamblerBet.awayTeamBet,
                onValueChange = { newAwayTeamBet ->
                    onBetChanged(partialPoolGamblerBet.copy(awayTeamBet = newAwayTeamBet))
                },
                modifier = Modifier
                    .scoreWidth()
                    .then(shimmerModifier),
            )
        }
    }
}

@Composable
fun PendingBetPlaceholderItem(modifier: Modifier = Modifier) {
    PendingBetItem(
        poolGamblerBet = poolGamblerBetFakeModel(),
        viewState = PendingBetItemViewState.Visualization(partialPoolGamblerBetFakeModel()),
        modifier = modifier,
        shimmerModifier = Modifier.shimmer(),
    )
}

private val flagSize = 32.dp

@Preview
@Composable
private fun NonEditablePendingBetItemPreview() {
    MaterialTheme {
        Surface {
            NonEditablePendingBetItem(
                modifier = Modifier.fillMaxWidth(),
                poolGamblerBet = poolGamblerBetDummyModel(),
                partialPoolGamblerBet = partialPoolGamblerBetDummyModel(),
            )
        }
    }
}

@Preview
@Composable
private fun EditablePendingBetItemPreview() {
    MaterialTheme {
        Surface {
            EditablePendingBetItem(
                modifier = Modifier.fillMaxWidth(),
                poolGamblerBet = poolGamblerBetDummyModel(),
                partialPoolGamblerBet = partialPoolGamblerBetDummyModel(),
                onBetChanged = {},
            )
        }
    }
}

@Preview
@Composable
private fun PendingBetPlaceholderItemPreview() {
    TycheTheme {
        Surface {
            PendingBetPlaceholderItem(modifier = Modifier.fillMaxWidth())
        }
    }
}
