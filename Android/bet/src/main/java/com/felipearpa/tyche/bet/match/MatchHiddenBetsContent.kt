package com.felipearpa.tyche.bet.match

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.R
import com.felipearpa.tyche.bet.finished.HistoryPointsRetryButton
import com.felipearpa.tyche.bet.finished.HistoryStyle
import com.felipearpa.tyche.bet.historyBetPreviewModels
import com.felipearpa.tyche.bet.matchOwnBetPlaceholderScore
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.LocalLoadingPlaceholderPulse
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.revenuecat.placeholder.placeholder
import com.felipearpa.tyche.ui.R as SharedR

/**
 * The body of the match screen while betting is open: a neutral closed lock, the hidden-bets
 * heading and explanation, and the signed-in viewer's own prediction. Nobody else's prediction
 * appears here. The lock is decorative; the heading and explanation are one announcement, and the
 * own prediction is another.
 */
@Composable
fun MatchHiddenBetsContent(
    match: PoolGamblerBetModel,
    ownBet: OwnBetState,
    onOwnBetRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lockSize: Dp = minOf(LOCK_SIZE * LocalDensity.current.fontScale, MAXIMUM_LOCK_SIZE)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(GROUP_SPACING),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
        ) {
            Icon(
                painter = painterResource(SharedR.drawable.lock),
                contentDescription = null,
                tint = HistoryStyle.secondaryText,
                modifier = Modifier.size(lockSize),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
                modifier = Modifier.semantics(mergeDescendants = true) {},
            ) {
                Text(
                    text = stringResource(R.string.match_bets_hidden_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = HistoryStyle.primaryText,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.match_bets_hidden_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = HistoryStyle.secondaryText,
                    textAlign = TextAlign.Center,
                )
            }
        }

        MatchOwnBetRegion(match = match, state = ownBet, onRetry = onOwnBetRetry)
    }
}

/**
 * The viewer's own prediction while bets are hidden. Loading renders the production line from a
 * placeholder score; a failure offers a retry that requests only this prediction; a confirmed
 * absence says "No bet". None of these is shown as another.
 */
@Composable
fun MatchOwnBetRegion(
    match: PoolGamblerBetModel,
    state: OwnBetState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        OwnBetState.Loading ->
            MatchOwnBetLine(match = match, betScore = matchOwnBetPlaceholderScore, isPlaceholder = true, modifier = modifier)

        is OwnBetState.Bet -> MatchOwnBetLine(match = match, betScore = state.betScore, modifier = modifier)
        OwnBetState.NoBet -> MatchOwnBetLine(match = match, betScore = null, modifier = modifier)
        OwnBetState.Failed -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
            modifier = modifier,
        ) {
            Text(
                text = stringResource(R.string.match_bets_own_bet_failure),
                style = MaterialTheme.typography.bodyMedium,
                color = HistoryStyle.secondaryText,
                modifier = Modifier.weight(1f, fill = false),
            )
            HistoryPointsRetryButton(onClick = onRetry)
        }
    }
}

/** "Your bet" with the viewer's prediction, or "No bet" after a confirmed absence. */
@Composable
private fun MatchOwnBetLine(
    match: PoolGamblerBetModel,
    betScore: TeamScore<Int>?,
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
) {
    val label = stringResource(R.string.history_your_bet_label)
    val value = betScore?.let { "${it.homeTeamValue} – ${it.awayTeamValue}" }
        ?: stringResource(R.string.history_no_bet_label)
    val announcement = ownBetAnnouncement(match, betScore)
    val secondary = HistoryStyle.secondaryText
    val primary = HistoryStyle.primaryText
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = secondary)) { append(label) }
        append("  ")
        withStyle(SpanStyle(color = primary, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")) {
            append(value)
        }
    }
    val mask = if (isPlaceholder) {
        val pulse = LocalLoadingPlaceholderPulse.current
        Modifier.placeholder(color = pulse.color, shape = pulse.shape, highlight = pulse.highlight)
    } else {
        Modifier
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clearAndSetSemantics { if (!isPlaceholder) contentDescription = announcement }
            .then(mask),
    )
}

@Composable
private fun ownBetAnnouncement(match: PoolGamblerBetModel, betScore: TeamScore<Int>?): String =
    if (betScore == null) {
        stringResource(R.string.history_no_bet_accessibility)
    } else {
        stringResource(
            R.string.match_bets_own_bet_accessibility,
            match.homeTeamName,
            betScore.homeTeamValue,
            match.awayTeamName,
            betScore.awayTeamValue,
        )
    }

private val GROUP_SPACING = 24.dp
private val LOCK_SIZE = 48.dp
private val MAXIMUM_LOCK_SIZE = 80.dp

@PreviewLightDark
@Preview(locale = "es-rCO", fontScale = 2f)
@Composable
private fun MatchHiddenBetsContentPreview() {
    val match = historyBetPreviewModels()[1].copy(isLocked = false, isComputed = false)
    TycheTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(32.dp), modifier = Modifier.padding(16.dp)) {
                MatchHiddenBetsContent(match = match, ownBet = OwnBetState.Bet(TeamScore(3, 2)), onOwnBetRetry = {})
                MatchOwnBetRegion(match = match, state = OwnBetState.NoBet, onRetry = {})
                MatchOwnBetRegion(match = match, state = OwnBetState.Loading, onRetry = {})
                MatchOwnBetRegion(match = match, state = OwnBetState.Failed, onRetry = {})
            }
        }
    }
}
