package com.felipearpa.tyche.bet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.core.toLocalDateTimeString
import com.felipearpa.tyche.ui.theme.boxSpacing

@Composable
fun PoolGamblerBetItem(
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
    poolGamblerBet: PoolGamblerBetModel,
    viewState: PoolGamblerBetItemViewState,
    onBetChanged: (PartialPoolGamblerBetModel) -> Unit = {}
) {
    when (viewState) {
        is PoolGamblerBetItemViewState.Visualization ->
            NonEditablePoolGamblerBetItem(
                modifier = modifier,
                shimmerModifier = shimmerModifier,
                poolGamblerBet = poolGamblerBet,
                partialPoolGamblerBet = viewState.value
            )

        is PoolGamblerBetItemViewState.Edition ->
            EditablePoolGamblerBetItem(
                modifier = modifier,
                shimmerModifier = shimmerModifier,
                poolGamblerBet = poolGamblerBet,
                partialPoolGamblerBet = viewState.value,
                onBetChanged = onBetChanged
            )
    }
}

@Composable
fun NonEditablePoolGamblerBetItem(
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
    poolGamblerBet: PoolGamblerBetModel,
    partialPoolGamblerBet: PartialPoolGamblerBetModel
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.boxSpacing.medium),
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = poolGamblerBet.homeTeamName, modifier = shimmerModifier)
            Text(text = partialPoolGamblerBet.homeTeamBet, modifier = shimmerModifier)
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = poolGamblerBet.awayTeamName, modifier = shimmerModifier)
            Text(text = partialPoolGamblerBet.awayTeamBet, modifier = shimmerModifier)
        }

        Text(
            text = poolGamblerBet.matchDateTime.toLocalDateTimeString(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(start = MaterialTheme.boxSpacing.medium)
                .then(shimmerModifier)
        )
    }
}

@Composable
fun EditablePoolGamblerBetItem(
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
    poolGamblerBet: PoolGamblerBetModel,
    partialPoolGamblerBet: PartialPoolGamblerBetModel,
    onBetChanged: (PartialPoolGamblerBetModel) -> Unit = {}
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.boxSpacing.medium),
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = poolGamblerBet.homeTeamName, modifier = shimmerModifier)
            BetTextField(
                value = partialPoolGamblerBet.homeTeamBet,
                onValueChange = { newHomeTeamBet ->
                    onBetChanged(partialPoolGamblerBet.copy(homeTeamBet = newHomeTeamBet))
                },
                modifier = Modifier
                    .betTextField()
                    .then(shimmerModifier)
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = poolGamblerBet.awayTeamName, modifier = shimmerModifier)
            BetTextField(
                value = partialPoolGamblerBet.awayTeamBet,
                onValueChange = { newAwayTeamBet ->
                    onBetChanged(partialPoolGamblerBet.copy(awayTeamBet = newAwayTeamBet))
                },
                modifier = Modifier
                    .betTextField()
                    .then(shimmerModifier)
            )
        }
    }
}

fun Modifier.betTextField() = composed {
    val widthInDp = with(LocalDensity.current) { LocalTextStyle.current.fontSize.toDp() } * 3
    Modifier.width(widthInDp)
}

@Preview
@Composable
private fun NonEditablePoolGamblerBetItemPreview() {
    MaterialTheme {
        Surface {
            NonEditablePoolGamblerBetItem(
                modifier = Modifier.fillMaxWidth(),
                poolGamblerBet = poolGamblerBetDummyModel(),
                partialPoolGamblerBet = partialPoolGamblerBetDummyModel()
            )
        }
    }
}

@Preview
@Composable
private fun EditablePoolGamblerBetItemPreview() {
    MaterialTheme {
        Surface {
            EditablePoolGamblerBetItem(
                modifier = Modifier.fillMaxWidth(),
                poolGamblerBet = poolGamblerBetDummyModel(),
                partialPoolGamblerBet = partialPoolGamblerBetDummyModel(),
                onBetChanged = {}
            )
        }
    }
}