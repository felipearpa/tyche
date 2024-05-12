package com.felipearpa.tyche.bet.finished

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.awayTeamBetRawValue
import com.felipearpa.tyche.bet.awayTeamMatchRawValue
import com.felipearpa.tyche.bet.homeTeamBetRawValue
import com.felipearpa.tyche.bet.homeTeamMatchRawValue
import com.felipearpa.tyche.bet.poolGamblerBetDummyModel
import com.felipearpa.tyche.bet.scoreWidth
import com.felipearpa.tyche.core.toLocalDateTimeString
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme

@Composable
fun FinishedBetItem(
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
    poolGamblerBet: PoolGamblerBetModel
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = poolGamblerBet.homeTeamName, modifier = shimmerModifier)
            Row(horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium)) {
                Text(
                    text = poolGamblerBet.homeTeamMatchRawValue(),
                    textAlign = TextAlign.Center,
                    modifier = shimmerModifier.scoreWidth()
                )
                Text(
                    text = poolGamblerBet.homeTeamBetRawValue(),
                    textAlign = TextAlign.Center,
                    modifier = shimmerModifier.betScoreStyle(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = poolGamblerBet.awayTeamName, modifier = shimmerModifier)
            Row(horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium)) {
                Text(
                    text = poolGamblerBet.awayTeamMatchRawValue(),
                    textAlign = TextAlign.Center,
                    modifier = shimmerModifier.scoreWidth()
                )
                Text(
                    text = poolGamblerBet.awayTeamBetRawValue(),
                    textAlign = TextAlign.Center,
                    modifier = shimmerModifier.betScoreStyle(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = poolGamblerBet.matchDateTime.toLocalDateTimeString(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = LocalBoxSpacing.current.large)
            )
            Text(
                text = poolGamblerBet.score?.toString().orEmpty(),
                modifier = shimmerModifier,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private fun Modifier.betScoreStyle() = composed {
    this
        .scoreWidth()
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.secondaryContainer)
}

@Preview(showBackground = true)
@Composable
private fun FinishedBetItemPreview() {
    TycheTheme {
        FinishedBetItem(
            poolGamblerBet = poolGamblerBetDummyModel(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}