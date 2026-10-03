package com.felipearpa.tyche.bet.finished

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.R
import com.felipearpa.tyche.bet.historyBetPlaceholderModel
import com.felipearpa.tyche.bet.historyBetPreviewModels
import com.felipearpa.tyche.ui.FlagImage
import com.felipearpa.tyche.ui.theme.LocalLoadingPlaceholderPulse
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.revenuecat.placeholder.placeholder

/**
 * One History match: the date and time, both teams around the final score, and the signed-in
 * gambler's bet with the awarded points. Loading slots render this same component from
 * [historyBetPlaceholderModel] with [isPlaceholder] set: each content leaf is then masked with the
 * shared [LocalLoadingPlaceholderPulse], the row ignores taps, and nothing reaches TalkBack.
 *
 * Another gambler's timeline keeps `FinishedBetItem`; this row's "Your bet" wording belongs to
 * the signed-in gambler's History only.
 */
@Composable
fun HistoryBetItem(
    poolGamblerBet: PoolGamblerBetModel,
    dateFormat: HistoryMatchDateFormat,
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val leafMask = leafMask(isPlaceholder)
    val dateText = dateFormat.date(poolGamblerBet.matchDateTime)
    val timeText = dateFormat.time(poolGamblerBet.matchDateTime)
    val announcement = historyBetAnnouncement(poolGamblerBet, dateText, timeText)

    val interaction = if (!isPlaceholder && onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    // One announcement per real row; a placeholder row contributes nothing.
    val semantics = Modifier.clearAndSetSemantics {
        if (!isPlaceholder) contentDescription = announcement
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
        modifier = modifier
            .then(interaction)
            .then(semantics)
            .padding(horizontal = HistoryStyle.horizontalPadding, vertical = VERTICAL_PADDING),
    ) {
        DateTimeLine(dateText = dateText, timeText = timeText, leafMask = leafMask)
        if (LocalDensity.current.fontScale >= STACKED_FONT_SCALE) {
            StackedMatchup(poolGamblerBet = poolGamblerBet, leafMask = leafMask)
        } else {
            Matchup(poolGamblerBet = poolGamblerBet, leafMask = leafMask)
        }
        Footer(poolGamblerBet = poolGamblerBet, leafMask = leafMask, isPlaceholder = isPlaceholder)
    }
}

/** The separator below each History row, inside the row gutter. */
@Composable
internal fun HistoryRowDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = HistoryStyle.horizontalPadding))
}

/** The shared pulse for one content leaf, in [shape] or the pulse's text shape. */
@Composable
private fun leafMask(isPlaceholder: Boolean, shape: Shape? = null): Modifier {
    if (!isPlaceholder) return Modifier
    val pulse = LocalLoadingPlaceholderPulse.current
    return Modifier.placeholder(
        color = pulse.color,
        shape = shape ?: pulse.shape,
        highlight = pulse.highlight,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DateTimeLine(dateText: String, timeText: String, leafMask: Modifier) {
    // Date leading and time trailing; the time moves below the date when both do not fit.
    FlowRow(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.bodyMedium,
            color = HistoryStyle.secondaryText,
            modifier = Modifier.padding(end = MINIMUM_GAP).then(leafMask),
        )
        Text(
            text = timeText,
            style = MaterialTheme.typography.bodyMedium,
            color = HistoryStyle.secondaryText,
            modifier = leafMask,
        )
    }
}

/**
 * Home on the left and away on the right of an intrinsic-width result; both team columns share
 * the remaining width, which keeps the result centered.
 */
@Composable
private fun Matchup(poolGamblerBet: PoolGamblerBetModel, leafMask: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MINIMUM_GAP),
        modifier = Modifier.fillMaxWidth(),
    ) {
        TeamColumn(teamId = poolGamblerBet.homeTeamId, name = poolGamblerBet.homeTeamName, leafMask = leafMask)
        Result(poolGamblerBet = poolGamblerBet, leafMask = leafMask)
        TeamColumn(teamId = poolGamblerBet.awayTeamId, name = poolGamblerBet.awayTeamName, leafMask = leafMask)
    }
}

/**
 * Large font scales leave no room for three columns, so the result sits between the home and
 * away teams vertically.
 */
@Composable
private fun StackedMatchup(poolGamblerBet: PoolGamblerBetModel, leafMask: Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(SECTION_SPACING), modifier = Modifier.fillMaxWidth()) {
        TeamLine(teamId = poolGamblerBet.homeTeamId, name = poolGamblerBet.homeTeamName, leafMask = leafMask)
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Result(poolGamblerBet = poolGamblerBet, leafMask = leafMask)
        }
        TeamLine(teamId = poolGamblerBet.awayTeamId, name = poolGamblerBet.awayTeamName, leafMask = leafMask)
    }
}

@Composable
private fun RowScope.TeamColumn(teamId: String, name: String, leafMask: Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FLAG_NAME_SPACING),
        modifier = Modifier.weight(1f),
    ) {
        Flag(teamId = teamId, leafMask = leafMask)
        TeamName(name = name, textAlign = TextAlign.Center, leafMask = leafMask)
    }
}

@Composable
private fun TeamLine(teamId: String, name: String, leafMask: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FLAG_NAME_SPACING * 2),
    ) {
        Flag(teamId = teamId, leafMask = leafMask)
        TeamName(name = name, textAlign = TextAlign.Start, leafMask = leafMask)
    }
}

/** Flags grow with the font scale up to a cap, leaving the width to the team names. */
@Composable
private fun Flag(teamId: String, leafMask: Modifier) {
    val width: Dp = minOf(FLAG_WIDTH * LocalDensity.current.fontScale, MAXIMUM_FLAG_WIDTH)
    FlagImage(
        teamId = teamId,
        modifier = Modifier
            .size(width = width, height = width * FLAG_ASPECT_RATIO)
            .then(leafMask),
    )
}

@Composable
private fun TeamName(name: String, textAlign: TextAlign, leafMask: Modifier) {
    Text(
        text = name,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = HistoryStyle.primaryText,
        textAlign = textAlign,
        modifier = leafMask,
    )
}

@Composable
private fun Result(poolGamblerBet: PoolGamblerBetModel, leafMask: Modifier) {
    // An unavailable result shows neutral dashes rather than a score.
    val scoreColor =
        if (poolGamblerBet.matchScore == null) HistoryStyle.secondaryText else HistoryStyle.primaryText
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SCORE_SPACING),
    ) {
        ScoreText(text = scoreText(poolGamblerBet.matchScore?.homeTeamValue), color = scoreColor, leafMask = leafMask)
        Text(
            text = "–",
            fontSize = SCORE_SIZE * SEPARATOR_SCALE,
            color = HistoryStyle.secondaryText,
            maxLines = 1,
            modifier = leafMask,
        )
        ScoreText(text = scoreText(poolGamblerBet.matchScore?.awayTeamValue), color = scoreColor, leafMask = leafMask)
    }
}

@Composable
private fun ScoreText(text: String, color: Color, leafMask: Modifier) {
    Text(
        text = text,
        fontSize = SCORE_SIZE,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = TABULAR_NUMBERS),
        modifier = leafMask,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Footer(poolGamblerBet: PoolGamblerBetModel, leafMask: Modifier, isPlaceholder: Boolean) {
    // The bet leading and the pill trailing; the pill moves below the bet when both do not fit.
    FlowRow(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
        itemVerticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        BetLine(poolGamblerBet = poolGamblerBet, leafMask = leafMask)
        PointsPill(points = HistoryPoints.of(poolGamblerBet.score), isPlaceholder = isPlaceholder)
    }
}

@Composable
private fun BetLine(poolGamblerBet: PoolGamblerBetModel, leafMask: Modifier) {
    val betScore = poolGamblerBet.betScore
    if (betScore == null) {
        Text(
            text = stringResource(R.string.history_no_bet_label),
            style = MaterialTheme.typography.bodyLarge,
            color = HistoryStyle.secondaryText,
            modifier = Modifier.padding(end = MINIMUM_GAP).then(leafMask),
        )
        return
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(BET_LABEL_SPACING),
        modifier = Modifier.padding(end = MINIMUM_GAP),
    ) {
        Text(
            text = stringResource(R.string.history_your_bet_label),
            style = MaterialTheme.typography.bodyLarge,
            color = HistoryStyle.secondaryText,
            modifier = Modifier.alignByBaseline().then(leafMask),
        )
        Text(
            text = "${betScore.homeTeamValue} – ${betScore.awayTeamValue}",
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR_NUMBERS),
            fontWeight = FontWeight.SemiBold,
            color = HistoryStyle.primaryText,
            modifier = Modifier.alignByBaseline().then(leafMask),
        )
    }
}

@Composable
private fun PointsPill(points: HistoryPoints, isPlaceholder: Boolean) {
    val isPositive = points.isPositive
    // A placeholder masks the whole pill once, rather than its fill and its text separately.
    val fill = if (isPlaceholder) Modifier else Modifier.background(
        color = if (isPositive) HistoryStyle.positiveFill else HistoryStyle.neutralFill,
        shape = CircleShape,
    )
    Text(
        text = historyPointsShortText(points),
        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR_NUMBERS),
        fontWeight = FontWeight.SemiBold,
        color = if (isPositive) HistoryStyle.positiveText else HistoryStyle.secondaryText,
        maxLines = 1,
        modifier = leafMask(isPlaceholder, CircleShape)
            .then(fill)
            .padding(horizontal = PILL_HORIZONTAL_PADDING, vertical = PILL_VERTICAL_PADDING),
    )
}

private fun scoreText(value: Int?): String = value?.toString() ?: "—"

/** The row's single TalkBack announcement: date and time, result, bet, and points, in order. */
@Composable
internal fun historyBetAnnouncement(poolGamblerBet: PoolGamblerBetModel, dateText: String, timeText: String): String {
    val home = poolGamblerBet.homeTeamName
    val away = poolGamblerBet.awayTeamName
    val matchScore = poolGamblerBet.matchScore
    val result = if (matchScore == null) {
        stringResource(R.string.history_result_unavailable_accessibility, home, away)
    } else {
        stringResource(
            R.string.history_result_accessibility,
            home,
            matchScore.homeTeamValue,
            away,
            matchScore.awayTeamValue,
        )
    }
    val betScore = poolGamblerBet.betScore
    val bet = if (betScore == null) {
        stringResource(R.string.history_no_bet_accessibility)
    } else {
        stringResource(R.string.history_bet_accessibility, betScore.homeTeamValue, betScore.awayTeamValue)
    }
    val score = poolGamblerBet.score
    val points = if (score == null) {
        stringResource(R.string.history_points_unavailable_accessibility)
    } else {
        pluralStringResource(R.plurals.history_points_accessibility, score, score)
    }
    return joinSentences(listOf("$dateText, $timeText", result, bet, points))
}

/**
 * Joins announcement parts as sentences. A part that already ends with a period, such as the
 * Spanish "p. m.", is not given a second one.
 */
internal fun joinSentences(parts: List<String>): String =
    parts.reduce { text, part -> if (text.endsWith('.')) "$text $part" else "$text. $part" }

private val SECTION_SPACING = 12.dp
private val VERTICAL_PADDING = 20.dp
private val MINIMUM_GAP = 12.dp
private val FLAG_NAME_SPACING = 6.dp
private val FLAG_WIDTH = 36.dp
private val MAXIMUM_FLAG_WIDTH = 52.dp
private const val FLAG_ASPECT_RATIO = 0.75f
private val SCORE_SIZE = 44.sp
private const val SEPARATOR_SCALE = 0.6f
private val SCORE_SPACING = 12.dp
private val BET_LABEL_SPACING = 8.dp
private val PILL_HORIZONTAL_PADDING = 14.dp
private val PILL_VERTICAL_PADDING = 6.dp
private const val TABULAR_NUMBERS = "tnum"

/** At or above this font scale the teams stack around the result. */
private const val STACKED_FONT_SCALE = 1.5f

@PreviewLightDark
@Preview(locale = "es-rCO")
@Preview(fontScale = 2f)
@Composable
private fun HistoryBetItemPreview() {
    val dateFormat = rememberHistoryMatchDateFormat()
    TycheTheme {
        Surface {
            Column {
                historyBetPreviewModels().forEach { bet ->
                    HistoryBetItem(poolGamblerBet = bet, dateFormat = dateFormat, onClick = {})
                    HistoryRowDivider()
                }
                HistoryBetItem(
                    poolGamblerBet = historyBetPlaceholderModel(),
                    dateFormat = dateFormat,
                    isPlaceholder = true,
                )
            }
        }
    }
}
