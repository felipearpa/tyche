package com.felipearpa.tyche.bet.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.foundation.time.toShortTimeString
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.awayTeamBetRawValue
import com.felipearpa.tyche.bet.homeTeamBetRawValue
import com.felipearpa.tyche.bet.poolGamblerBetDummyModel
import com.felipearpa.tyche.bet.scoreWidth
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing

@Composable
fun LiveBetItem(
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
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = poolGamblerBet.homeTeamName, modifier = shimmerModifier)
            Spacer(modifier = Modifier.weight(1f))
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

@Preview(showBackground = true)
@Composable
private fun LiveBetItemPreview() {
    LiveBetItem(
        poolGamblerBet = poolGamblerBetDummyModel(),
        modifier = Modifier.fillMaxWidth(),
    )
}
