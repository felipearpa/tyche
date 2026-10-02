package com.felipearpa.tyche.bet.finished

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
import com.felipearpa.tyche.bet.awayTeamMatchRawValue
import com.felipearpa.tyche.bet.homeTeamBetRawValue
import com.felipearpa.tyche.bet.homeTeamMatchRawValue
import com.felipearpa.tyche.bet.poolGamblerBetDummyModel
import com.felipearpa.tyche.bet.poolGamblerBetFakeModel
import com.felipearpa.tyche.bet.scoreWidth
import com.felipearpa.tyche.ui.FlagImage
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.LocalLoadingPlaceholderPulse
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.revenuecat.placeholder.placeholder

/**
 * A computed bet with the match result and the points earned. Loading rows render this same
 * component from `poolGamblerBetFakeModel()` with [isPlaceholder] set; each content leaf is then
 * masked with the shared [LocalLoadingPlaceholderPulse] and nothing reaches TalkBack.
 */
@Composable
fun FinishedBetItem(
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
        Text(
            text = poolGamblerBet.matchDateTime.toShortTimeString(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            modifier = leafMask,
        )

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
                text = poolGamblerBet.homeTeamMatchRawValue(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .scoreWidth()
                    .then(leafMask),
            )
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
                text = poolGamblerBet.awayTeamMatchRawValue(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .scoreWidth()
                    .then(leafMask),
            )
            Text(
                text = poolGamblerBet.awayTeamBetRawValue(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .scoreWidth()
                    .then(leafMask),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = "+${poolGamblerBet.score?.toString().orEmpty()}",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.End,
                modifier = leafMask,
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

@Preview
@Composable
private fun FinishedBetPlaceholderItemPreview() {
    TycheTheme {
        Surface {
            FinishedBetItem(
                poolGamblerBet = poolGamblerBetFakeModel(),
                modifier = Modifier.fillMaxWidth(),
                isPlaceholder = true,
            )
        }
    }
}

private val flagSize = 24.dp
