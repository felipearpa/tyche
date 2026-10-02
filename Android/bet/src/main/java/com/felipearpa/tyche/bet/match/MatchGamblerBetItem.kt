package com.felipearpa.tyche.bet.match

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.awayTeamBetRawValue
import com.felipearpa.tyche.bet.hasBet
import com.felipearpa.tyche.bet.homeTeamBetRawValue
import com.felipearpa.tyche.bet.poolGamblerBetDummyModel
import com.felipearpa.tyche.bet.poolGamblerBetFakeModel
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.LocalLoadingPlaceholderPulse
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.revenuecat.placeholder.placeholder

/**
 * One gambler's bet on the match being viewed. Loading rows render this same component from
 * `poolGamblerBetFakeModel()` with [isPlaceholder] set; each content leaf is then masked with
 * the shared [LocalLoadingPlaceholderPulse] and nothing reaches TalkBack. Callers attach no
 * click action to placeholders.
 */
@Composable
fun MatchGamblerBetItem(
    poolGamblerBet: PoolGamblerBetModel,
    modifier: Modifier = Modifier,
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

    Row(
        // Placeholder values are filler, not bets: keep them from screen readers.
        modifier = if (isPlaceholder) modifier.clearAndSetSemantics {} else modifier,
        horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = poolGamblerBet.gamblerUsername,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .then(leafMask),
        )

        if (poolGamblerBet.hasBet()) {
            Text(
                text = "${poolGamblerBet.homeTeamBetRawValue()} - ${poolGamblerBet.awayTeamBetRawValue()}",
                style = MaterialTheme.typography.titleMedium,
                modifier = leafMask,
            )
        }

        Spacer(modifier = Modifier.width(LocalBoxSpacing.current.medium))

        Text(
            text = poolGamblerBet.score?.let { "+$it" }.orEmpty(),
            style = MaterialTheme.typography.titleLarge,
            modifier = leafMask,
        )
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

@PreviewLightDark
@Composable
private fun MatchGamblerBetPlaceholderPreview() {
    TycheTheme {
        Surface {
            MatchGamblerBetItem(
                poolGamblerBet = poolGamblerBetFakeModel(),
                modifier = Modifier.fillMaxWidth(),
                isPlaceholder = true,
            )
        }
    }
}
