package com.felipearpa.tyche.bet.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.felipearpa.foundation.time.toShortTimeString
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.awayTeamBetRawValue
import com.felipearpa.tyche.bet.homeTeamBetRawValue
import com.felipearpa.tyche.bet.poolGamblerBetDummyModel
import com.felipearpa.tyche.bet.poolGamblerBetFakeModel
import com.felipearpa.tyche.bet.scoreWidth
import com.felipearpa.tyche.ui.FlagImage
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.LocalLoadingPlaceholderPulse
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.revenuecat.placeholder.placeholder

/**
 * A bet on a match that has started but is not computed yet. Loading rows render this same
 * component from `poolGamblerBetFakeModel()` with [isPlaceholder] set; each content leaf is then
 * masked with the shared [LocalLoadingPlaceholderPulse] and nothing reaches TalkBack.
 */
@Composable
fun LiveBetItem(
    modifier: Modifier = Modifier,
    poolGamblerBet: PoolGamblerBetModel,
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

    Column(
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
        // Placeholder values are filler, not bets: keep them from screen readers.
        modifier = if (isPlaceholder) modifier.clearAndSetSemantics {} else modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = poolGamblerBet.matchDateTime.toShortTimeString(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                modifier = leafMask,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
        ) {
            FlagImage(
                teamId = poolGamblerBet.homeTeamId,
                modifier = Modifier
                    .size(flagSize)
                    .then(leafMask),
            )
            Text(text = poolGamblerBet.homeTeamName, modifier = leafMask)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = poolGamblerBet.homeTeamBetRawValue(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .scoreWidth()
                    .then(leafMask),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
        ) {
            FlagImage(
                teamId = poolGamblerBet.awayTeamId,
                modifier = Modifier
                    .size(flagSize)
                    .then(leafMask),
            )
            Text(text = poolGamblerBet.awayTeamName, modifier = leafMask)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = poolGamblerBet.awayTeamBetRawValue(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .scoreWidth()
                    .then(leafMask),
            )
        }
    }
}

private val flagSize = 32.dp

@PreviewLightDark
@Composable
private fun LiveBetItemPreview() {
    TycheTheme {
        Surface {
            LiveBetItem(
                poolGamblerBet = poolGamblerBetDummyModel(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview
@Composable
private fun LiveBetPlaceholderItemPreview() {
    TycheTheme {
        Surface {
            LiveBetItem(
                poolGamblerBet = poolGamblerBetFakeModel(),
                modifier = Modifier.fillMaxWidth(),
                isPlaceholder = true,
            )
        }
    }
}
