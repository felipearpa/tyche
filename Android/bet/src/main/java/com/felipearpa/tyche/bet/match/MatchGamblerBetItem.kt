package com.felipearpa.tyche.bet.match

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.awayTeamBetRawValue
import com.felipearpa.tyche.bet.homeTeamBetRawValue
import com.felipearpa.tyche.bet.poolGamblerBetDummyModel
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme

@Composable
fun MatchGamblerBetItem(
    poolGamblerBet: PoolGamblerBetModel,
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = poolGamblerBet.gamblerUsername,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).then(shimmerModifier),
        )

        Text(
            text = "${poolGamblerBet.homeTeamBetRawValue()} - ${poolGamblerBet.awayTeamBetRawValue()}",
            style = MaterialTheme.typography.titleMedium,
            modifier = shimmerModifier,
        )

        poolGamblerBet.score?.let { score ->
            Spacer(modifier = Modifier)
            Text(
                text = "+$score",
                style = MaterialTheme.typography.titleMedium,
                modifier = shimmerModifier,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun MatchGamblerBetItemPreview() {
    TycheTheme {
        Surface {
            MatchGamblerBetItem(
                poolGamblerBet = poolGamblerBetDummyModel(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
