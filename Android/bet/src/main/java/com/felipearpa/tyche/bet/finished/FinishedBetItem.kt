package com.felipearpa.tyche.bet.finished

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.felipearpa.foundation.time.toShortTimeString
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.awayTeamBetRawValue
import com.felipearpa.tyche.bet.awayTeamMatchRawValue
import com.felipearpa.tyche.bet.homeTeamBetRawValue
import com.felipearpa.tyche.bet.homeTeamMatchRawValue
import com.felipearpa.tyche.bet.poolGamblerBetDummyModel
import com.felipearpa.tyche.bet.scoreWidth
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme

@Composable
fun FinishedBetItem(
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
    poolGamblerBet: PoolGamblerBetModel,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = poolGamblerBet.matchDateTime.toShortTimeString(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                modifier = shimmerModifier,
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "+${poolGamblerBet.score?.toString().orEmpty()}",
                modifier = shimmerModifier,
                style = MaterialTheme.typography.titleLarge,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = poolGamblerBet.homeTeamName, modifier = shimmerModifier)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = poolGamblerBet.homeTeamMatchRawValue(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .scoreWidth()
                    .then(shimmerModifier),
            )
            Text(
                text = poolGamblerBet.homeTeamBetRawValue(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .scoreWidth()
                    .then(shimmerModifier),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = poolGamblerBet.awayTeamName, modifier = shimmerModifier)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = poolGamblerBet.awayTeamMatchRawValue(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .scoreWidth()
                    .then(shimmerModifier),
            )
            Text(
                text = poolGamblerBet.awayTeamBetRawValue(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .scoreWidth()
                    .then(shimmerModifier),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FinishedBetItemPreview() {
    TycheTheme {
        Surface {
            FinishedBetItem(
                poolGamblerBet = poolGamblerBetDummyModel(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
