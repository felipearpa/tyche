package com.felipearpa.tyche.bet.pending

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    val isEdition = viewState is PendingBetItemViewState.Edition
    val rowSpacing by animateDpAsState(
        targetValue = if (isEdition) LocalBoxSpacing.current.small else LocalBoxSpacing.current.medium,
        label = "rowSpacing",
    )

    Column(
        modifier = modifier.animateContentSize(
            animationSpec = spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = Spring.DampingRatioNoBouncy,
            ),
        ),
        verticalArrangement = Arrangement.spacedBy(rowSpacing),
    ) {
        TeamRow(
            teamId = poolGamblerBet.homeTeamId,
            teamName = poolGamblerBet.homeTeamName,
            bet = viewState.value.homeTeamBet,
            isEdition = isEdition,
            onBetChange = { newHomeTeamBet ->
                onBetChanged(viewState.value.copy(homeTeamBet = newHomeTeamBet))
            },
            shimmerModifier = shimmerModifier,
        )

        TeamRow(
            teamId = poolGamblerBet.awayTeamId,
            teamName = poolGamblerBet.awayTeamName,
            bet = viewState.value.awayTeamBet,
            isEdition = isEdition,
            onBetChange = { newAwayTeamBet ->
                onBetChanged(viewState.value.copy(awayTeamBet = newAwayTeamBet))
            },
            shimmerModifier = shimmerModifier,
        )

        AnimatedVisibility(
            visible = !isEdition,
            enter = fadeIn() + expandVertically(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Text(
                text = poolGamblerBet.matchDateTime.toShortDateTimeString(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(start = LocalBoxSpacing.current.large)
                    .then(shimmerModifier),
            )
        }
    }
}

@Composable
private fun TeamRow(
    teamId: String,
    teamName: String,
    bet: String,
    isEdition: Boolean,
    onBetChange: (String) -> Unit,
    shimmerModifier: Modifier,
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
                teamId = teamId,
                modifier = Modifier
                    .size(flagSize)
                    .then(shimmerModifier),
            )
            Text(text = teamName, modifier = shimmerModifier)
        }

        AnimatedContent(
            targetState = isEdition,
            transitionSpec = {
                (fadeIn(animationSpec = tween(150)) togetherWith
                    fadeOut(animationSpec = tween(150)))
                    .using(SizeTransform(clip = false))
            },
            label = "scoreCell",
        ) { editing ->
            if (editing) {
                BetTextField(
                    value = bet,
                    onValueChange = onBetChange,
                    modifier = Modifier
                        .scoreWidth()
                        .then(shimmerModifier),
                )
            } else {
                Text(text = bet, modifier = shimmerModifier)
            }
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
            PendingBetItem(
                modifier = Modifier.fillMaxWidth(),
                poolGamblerBet = poolGamblerBetDummyModel(),
                viewState = PendingBetItemViewState.Visualization(
                    partialPoolGamblerBetDummyModel(),
                ),
            )
        }
    }
}

@Preview
@Composable
private fun EditablePendingBetItemPreview() {
    MaterialTheme {
        Surface {
            PendingBetItem(
                modifier = Modifier.fillMaxWidth(),
                poolGamblerBet = poolGamblerBetDummyModel(),
                viewState = PendingBetItemViewState.Edition(
                    partialPoolGamblerBetDummyModel(),
                ),
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
