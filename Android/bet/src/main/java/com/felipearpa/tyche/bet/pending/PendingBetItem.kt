package com.felipearpa.tyche.bet.pending

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.foundation.time.toShortDateTimeString
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.poolGamblerBetDummyModel
import com.felipearpa.tyche.bet.poolGamblerBetFakeModel
import com.felipearpa.tyche.bet.scoreWidth
import com.felipearpa.tyche.ui.FlagImage
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.LocalLoadingPlaceholderPulse
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.revenuecat.placeholder.placeholder

/**
 * A bet on a match that has not started. Loading rows render this same component from
 * `poolGamblerBetFakeModel()` and `partialPoolGamblerBetFakeModel()` with [isPlaceholder] set;
 * each content leaf is then masked with the shared [LocalLoadingPlaceholderPulse] and nothing
 * reaches TalkBack. Placeholders are never editable.
 */
@Composable
fun PendingBetItem(
    modifier: Modifier = Modifier,
    poolGamblerBet: PoolGamblerBetModel,
    viewState: PendingBetItemViewState,
    onBetChanged: (PartialPoolGamblerBetModel) -> Unit = {},
    isPlaceholder: Boolean = false,
) {
    // Applied to each content leaf separately, never to the whole row.
    val leafMask = if (isPlaceholder) {
        val pulse = LocalLoadingPlaceholderPulse.current
        Modifier.placeholder(
            color = pulse.color,
            shape = pulse.shape,
            highlight = pulse.highlight,
        )
    } else {
        Modifier
    }
    val isEdition = viewState is PendingBetItemViewState.Edition
    val rowSpacing by animateDpAsState(
        targetValue = if (isEdition) LocalBoxSpacing.current.small else LocalBoxSpacing.current.medium,
        label = "rowSpacing",
    )

    Column(
        modifier = modifier
            // Placeholder values are filler, not bets: keep them from screen readers.
            .then(if (isPlaceholder) Modifier.clearAndSetSemantics {} else Modifier)
            .animateContentSize(
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
            leafMask = leafMask,
        )

        TeamRow(
            teamId = poolGamblerBet.awayTeamId,
            teamName = poolGamblerBet.awayTeamName,
            bet = viewState.value.awayTeamBet,
            isEdition = isEdition,
            onBetChange = { newAwayTeamBet ->
                onBetChanged(viewState.value.copy(awayTeamBet = newAwayTeamBet))
            },
            leafMask = leafMask,
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
                    .then(leafMask),
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
    leafMask: Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
            modifier = Modifier.weight(1f),
        ) {
            FlagImage(
                teamId = teamId,
                modifier = Modifier
                    .size(flagSize)
                    .then(leafMask),
            )
            Text(text = teamName, modifier = leafMask)
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
                        .then(leafMask),
                )
            } else {
                Text(text = bet, modifier = leafMask)
            }
        }
    }
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
            PendingBetItem(
                poolGamblerBet = poolGamblerBetFakeModel(),
                viewState = PendingBetItemViewState.Visualization(partialPoolGamblerBetFakeModel()),
                modifier = Modifier.fillMaxWidth(),
                isPlaceholder = true,
            )
        }
    }
}
