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
import com.felipearpa.tyche.bet.timelineBetPreviewModels
import com.felipearpa.tyche.ui.FlagImage
import com.felipearpa.tyche.ui.theme.LocalLoadingPlaceholderPulse
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.revenuecat.placeholder.placeholder

/**
 * One match in History or another gambler's Timeline: the date and time, both teams around the
 * score, and the owner's bet with its points. Loading slots render this same component from
 * [historyBetPlaceholderModel] with [isPlaceholder] set: each content leaf is then masked with the
 * shared [LocalLoadingPlaceholderPulse], the row ignores taps, and nothing reaches TalkBack.
 *
 * [owner] selects the ownership wording; it defaults to the signed-in gambler's "Your bet". A
 * computed entry shows the final score and the awarded points. An entry whose points are not
 * computed yet shows the match score available so far (or dashes) and "Points pending", and never
 * describes that score as final or the match as live.
 */
@Composable
fun HistoryBetItem(
    poolGamblerBet: PoolGamblerBetModel,
    dateFormat: HistoryMatchDateFormat,
    modifier: Modifier = Modifier,
    owner: HistoryOwner = HistoryOwner.SignedInGambler,
    isPlaceholder: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val leafMask = leafMask(isPlaceholder)
    val dateText = dateFormat.date(poolGamblerBet.matchDateTime)
    val timeText = dateFormat.time(poolGamblerBet.matchDateTime)
    val announcement = historyBetAnnouncement(poolGamblerBet, owner, dateText, timeText)

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
        Footer(poolGamblerBet = poolGamblerBet, owner = owner, leafMask = leafMask, isPlaceholder = isPlaceholder)
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
private fun Footer(
    poolGamblerBet: PoolGamblerBetModel,
    owner: HistoryOwner,
    leafMask: Modifier,
    isPlaceholder: Boolean,
) {
    // The bet leading and the pill trailing; the pill moves below the bet when both do not fit.
    FlowRow(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
        itemVerticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        BetLine(poolGamblerBet = poolGamblerBet, owner = owner, leafMask = leafMask)
        PointsPill(points = HistoryRowPoints.of(poolGamblerBet), isPlaceholder = isPlaceholder)
    }
}

@Composable
private fun BetLine(poolGamblerBet: PoolGamblerBetModel, owner: HistoryOwner, leafMask: Modifier) {
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
            text = stringResource(
                when (owner) {
                    HistoryOwner.SignedInGambler -> R.string.history_your_bet_label
                    is HistoryOwner.SelectedGambler -> R.string.history_bet_label
                },
            ),
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
private fun PointsPill(points: HistoryRowPoints, isPlaceholder: Boolean) {
    val isPositive = points is HistoryRowPoints.Awarded && points.points.isPositive
    // A placeholder masks the whole pill once, rather than its fill and its text separately.
    val fill = if (isPlaceholder) Modifier else Modifier.background(
        color = if (isPositive) HistoryStyle.positiveFill else HistoryStyle.neutralFill,
        shape = CircleShape,
    )
    Text(
        text = when (points) {
            is HistoryRowPoints.Awarded -> historyPointsShortText(points.points)
            HistoryRowPoints.Pending -> stringResource(R.string.history_points_pending_label)
        },
        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR_NUMBERS),
        fontWeight = FontWeight.SemiBold,
        color = if (isPositive) HistoryStyle.positiveText else HistoryStyle.secondaryText,
        // A numeric award stays on one line; the pending words may wrap at large font scales.
        maxLines = if (points is HistoryRowPoints.Awarded) 1 else Int.MAX_VALUE,
        modifier = leafMask(isPlaceholder, CircleShape)
            .then(fill)
            .padding(horizontal = PILL_HORIZONTAL_PADDING, vertical = PILL_VERTICAL_PADDING),
    )
}

private fun scoreText(value: Int?): String = value?.toString() ?: "—"

/**
 * A row's points: the authoritative award once the entry is computed, otherwise pending. A
 * pending entry never shows its score value as an award.
 */
sealed interface HistoryRowPoints {
    data class Awarded(val points: HistoryPoints) : HistoryRowPoints
    data object Pending : HistoryRowPoints

    companion object {
        fun of(poolGamblerBet: PoolGamblerBetModel): HistoryRowPoints =
            if (poolGamblerBet.isComputed) Awarded(HistoryPoints.of(poolGamblerBet.score)) else Pending
    }
}

/** The row's single TalkBack announcement: date and time, score, bet, and points, in order. */
@Composable
internal fun historyBetAnnouncement(
    poolGamblerBet: PoolGamblerBetModel,
    owner: HistoryOwner,
    dateText: String,
    timeText: String,
): String = joinSentences(
    listOf(
        "$dateText, $timeText",
        resultAnnouncement(poolGamblerBet),
        betAnnouncement(poolGamblerBet, owner),
        pointsAnnouncement(HistoryRowPoints.of(poolGamblerBet)),
    ),
)

/**
 * A computed entry's score is the final result; any other score is only the match score reported
 * so far.
 */
@Composable
private fun resultAnnouncement(poolGamblerBet: PoolGamblerBetModel): String {
    val home = poolGamblerBet.homeTeamName
    val away = poolGamblerBet.awayTeamName
    val isFinal = poolGamblerBet.isComputed
    val matchScore = poolGamblerBet.matchScore ?: return stringResource(
        if (isFinal) R.string.history_result_unavailable_accessibility
        else R.string.history_match_score_unavailable_accessibility,
        home,
        away,
    )
    return stringResource(
        if (isFinal) R.string.history_result_accessibility else R.string.history_match_score_accessibility,
        home,
        matchScore.homeTeamValue,
        away,
        matchScore.awayTeamValue,
    )
}

@Composable
private fun betAnnouncement(poolGamblerBet: PoolGamblerBetModel, owner: HistoryOwner): String {
    val isSignedIn = owner == HistoryOwner.SignedInGambler
    val betScore = poolGamblerBet.betScore ?: return stringResource(
        if (isSignedIn) R.string.history_no_bet_accessibility else R.string.history_gambler_no_bet_accessibility,
    )
    return stringResource(
        if (isSignedIn) R.string.history_bet_accessibility else R.string.history_gambler_bet_accessibility,
        betScore.homeTeamValue,
        betScore.awayTeamValue,
    )
}

@Composable
private fun pointsAnnouncement(points: HistoryRowPoints): String = when (points) {
    HistoryRowPoints.Pending -> stringResource(R.string.history_points_pending_label)
    is HistoryRowPoints.Awarded -> when (val awarded = points.points) {
        is HistoryPoints.Earned -> pluralStringResource(R.plurals.history_points_accessibility, awarded.value, awarded.value)
        HistoryPoints.Unavailable -> stringResource(R.string.history_points_unavailable_accessibility)
    }
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

@PreviewLightDark
@Preview(locale = "es-rCO", fontScale = 2f)
@Composable
private fun HistoryBetItemSelectedGamblerPreview() {
    val dateFormat = rememberHistoryMatchDateFormat()
    TycheTheme {
        Surface {
            Column {
                timelineBetPreviewModels().forEach { bet ->
                    HistoryBetItem(
                        poolGamblerBet = bet,
                        dateFormat = dateFormat,
                        owner = HistoryOwner.SelectedGambler(name = "El mono"),
                        onClick = {},
                    )
                    HistoryRowDivider()
                }
            }
        }
    }
}
