package com.felipearpa.tyche.bet.match

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.R
import com.felipearpa.tyche.bet.finished.HistoryMatchDateFormat
import com.felipearpa.tyche.bet.finished.HistoryStyle
import com.felipearpa.tyche.bet.finished.joinSentences
import com.felipearpa.tyche.bet.finished.rememberHistoryMatchDateFormat
import com.felipearpa.tyche.bet.historyBetPreviewModels
import com.felipearpa.tyche.bet.matchHeaderPlaceholderModel
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.ui.FlagImage
import com.felipearpa.tyche.ui.theme.LocalLoadingPlaceholderPulse
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.revenuecat.placeholder.placeholder

/**
 * The match heading at the top of the match screen's scrolling content: a status line, then both
 * teams around the center.
 *
 * | Presentation | Status line | Center |
 * | --- | --- | --- |
 * | `HIDDEN` | "Starts" pill with the kickoff date and time | VS |
 * | `BETS_CLOSED` | "Bets closed" pill | VS, or the match score reported so far |
 * | `COMPUTED` | Kickoff date and time | Prominent final score, or dashes when unavailable |
 *
 * None of these claims that play is live. The heading is one TalkBack heading with one
 * announcement. Loading renders this same component from [matchHeaderPlaceholderModel] with
 * [isPlaceholder] set: each content leaf is then masked with the shared
 * [LocalLoadingPlaceholderPulse] and nothing reaches TalkBack.
 */
@Composable
fun MatchHeader(
    match: PoolGamblerBetModel,
    presentation: MatchBetsPresentation,
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
    dateFormat: HistoryMatchDateFormat = rememberHistoryMatchDateFormat(),
) {
    val leafMask = leafMask(isPlaceholder)
    val dateTimeText = "${dateFormat.date(match.matchDateTime)} · ${dateFormat.time(match.matchDateTime)}"
    val center = MatchHeaderCenter.of(match, presentation)
    val announcement = matchHeaderAnnouncement(match, presentation, center, dateTimeText)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                if (!isPlaceholder) {
                    contentDescription = announcement
                    heading()
                }
            },
    ) {
        StatusLine(presentation = presentation, dateTimeText = dateTimeText, isPlaceholder = isPlaceholder)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val fits = rememberMatchupFits(match = match, center = center, availableWidth = maxWidth)
            if (LocalDensity.current.fontScale >= STACKED_FONT_SCALE || !fits) {
                StackedMatchup(match = match, center = center, leafMask = leafMask)
            } else {
                Matchup(match = match, center = center, leafMask = leafMask)
            }
        }
    }
}

/**
 * Whether both team columns can sit beside the center without breaking a word of a team name, as
 * in a narrow window or with a long name. Otherwise the teams stack around the center.
 */
@Composable
private fun rememberMatchupFits(match: PoolGamblerBetModel, center: MatchHeaderCenter, availableWidth: Dp): Boolean {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val nameStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
    val centerText = centerWidthSample(center)
    val centerStyle = when (center) {
        MatchHeaderCenter.Versus -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black)
        is MatchHeaderCenter.MatchScore -> MaterialTheme.typography.displayLarge.copy(fontSize = MATCH_SCORE_SIZE, fontWeight = FontWeight.Bold)
        is MatchHeaderCenter.FinalScore -> MaterialTheme.typography.displayLarge.copy(fontSize = FINAL_SCORE_SIZE, fontWeight = FontWeight.Bold)
    }
    return remember(match.homeTeamName, match.awayTeamName, centerText, availableWidth, density, nameStyle, centerStyle) {
        fun width(text: String, style: TextStyle) = textMeasurer.measure(text, style, maxLines = 1, softWrap = false).size.width
        val longestWord = (match.homeTeamName.split(' ') + match.awayTeamName.split(' '))
            .maxOf { word -> width(word, nameStyle) }
        val gaps = with(density) { (MINIMUM_GAP * 2 + SCORE_SPACING * 2).roundToPx() }
        longestWord * 2 + width(centerText, centerStyle) + gaps <= with(density) { availableWidth.roundToPx() }
    }
}

/** Text as wide as the center, for measuring; the score separator is narrower than this. */
private fun centerWidthSample(center: MatchHeaderCenter): String = when (center) {
    MatchHeaderCenter.Versus -> "VS"
    is MatchHeaderCenter.MatchScore -> "${center.score.homeTeamValue}${center.score.awayTeamValue}–"
    is MatchHeaderCenter.FinalScore ->
        "${center.score?.homeTeamValue ?: "—"}${center.score?.awayTeamValue ?: "—"}–"
}

@Composable
private fun leafMask(isPlaceholder: Boolean): Modifier {
    if (!isPlaceholder) return Modifier
    val pulse = LocalLoadingPlaceholderPulse.current
    return Modifier.placeholder(color = pulse.color, shape = pulse.shape, highlight = pulse.highlight)
}

@Composable
private fun StatusLine(presentation: MatchBetsPresentation, dateTimeText: String, isPlaceholder: Boolean) {
    when (presentation) {
        MatchBetsPresentation.HIDDEN ->
            StatusPill(text = stringResource(R.string.match_bets_starts_label, dateTimeText), isPlaceholder = isPlaceholder)

        MatchBetsPresentation.BETS_CLOSED ->
            StatusPill(text = stringResource(R.string.match_bets_closed_label), isPlaceholder = isPlaceholder)

        MatchBetsPresentation.COMPUTED -> Text(
            text = dateTimeText,
            style = MaterialTheme.typography.bodyMedium,
            color = HistoryStyle.secondaryText,
            textAlign = TextAlign.Center,
            modifier = leafMask(isPlaceholder),
        )
    }
}

@Composable
private fun StatusPill(text: String, isPlaceholder: Boolean) {
    // A placeholder masks the whole pill once, rather than its fill and its text separately.
    val surface = if (isPlaceholder) {
        val pulse = LocalLoadingPlaceholderPulse.current
        Modifier.placeholder(color = pulse.color, shape = CircleShape, highlight = pulse.highlight)
    } else {
        Modifier.background(color = HistoryStyle.neutralFill, shape = CircleShape)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = HistoryStyle.primaryText,
        textAlign = TextAlign.Center,
        modifier = surface.padding(horizontal = PILL_HORIZONTAL_PADDING, vertical = PILL_VERTICAL_PADDING),
    )
}

/**
 * Home on the left and away on the right of an intrinsic-width center; both team columns share
 * the remaining width, which keeps the center in the middle.
 */
@Composable
private fun Matchup(match: PoolGamblerBetModel, center: MatchHeaderCenter, leafMask: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MINIMUM_GAP),
        modifier = Modifier.fillMaxWidth(),
    ) {
        TeamColumn(teamId = match.homeTeamId, name = match.homeTeamName, leafMask = leafMask)
        Center(center = center, leafMask = leafMask)
        TeamColumn(teamId = match.awayTeamId, name = match.awayTeamName, leafMask = leafMask)
    }
}

/**
 * Large font scales leave no room for three columns, so the center sits between the home and away
 * teams vertically.
 */
@Composable
private fun StackedMatchup(match: PoolGamblerBetModel, center: MatchHeaderCenter, leafMask: Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(SECTION_SPACING), modifier = Modifier.fillMaxWidth()) {
        TeamLine(teamId = match.homeTeamId, name = match.homeTeamName, leafMask = leafMask)
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Center(center = center, leafMask = leafMask)
        }
        TeamLine(teamId = match.awayTeamId, name = match.awayTeamName, leafMask = leafMask)
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
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = HistoryStyle.primaryText,
        textAlign = textAlign,
        modifier = leafMask,
    )
}

@Composable
private fun Center(center: MatchHeaderCenter, leafMask: Modifier) {
    when (center) {
        MatchHeaderCenter.Versus -> Text(
            text = stringResource(R.string.match_bets_versus_label),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = HistoryStyle.secondaryText,
            maxLines = 1,
            modifier = leafMask,
        )

        is MatchHeaderCenter.MatchScore ->
            Score(score = center.score, size = MATCH_SCORE_SIZE, leafMask = leafMask)

        is MatchHeaderCenter.FinalScore ->
            Score(score = center.score, size = FINAL_SCORE_SIZE, leafMask = leafMask)
    }
}

@Composable
private fun Score(score: TeamScore<Int>?, size: TextUnit, leafMask: Modifier) {
    // An unavailable result shows neutral dashes rather than a score.
    val digits = if (score == null) HistoryStyle.secondaryText else HistoryStyle.primaryText
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SCORE_SPACING),
    ) {
        ScoreText(text = score?.homeTeamValue?.toString() ?: "—", size = size, color = digits, leafMask = leafMask)
        Text(
            text = "–",
            fontSize = size * SEPARATOR_SCALE,
            color = HistoryStyle.secondaryText,
            maxLines = 1,
            modifier = leafMask,
        )
        ScoreText(text = score?.awayTeamValue?.toString() ?: "—", size = size, color = digits, leafMask = leafMask)
    }
}

@Composable
private fun ScoreText(text: String, size: TextUnit, color: Color, leafMask: Modifier) {
    Text(
        text = text,
        fontSize = size,
        lineHeight = size * SCORE_LINE_HEIGHT_SCALE,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
        modifier = leafMask,
    )
}

/** The heading's single announcement: the matchup or score, then the status line. */
@Composable
internal fun matchHeaderAnnouncement(
    match: PoolGamblerBetModel,
    presentation: MatchBetsPresentation,
    center: MatchHeaderCenter,
    dateTimeText: String,
): String {
    val home = match.homeTeamName
    val away = match.awayTeamName
    val matchup = when (center) {
        MatchHeaderCenter.Versus -> stringResource(R.string.match_bets_matchup_accessibility, home, away)
        is MatchHeaderCenter.MatchScore -> stringResource(
            R.string.history_match_score_accessibility,
            home,
            center.score.homeTeamValue,
            away,
            center.score.awayTeamValue,
        )

        is MatchHeaderCenter.FinalScore -> center.score?.let { score ->
            stringResource(R.string.history_result_accessibility, home, score.homeTeamValue, away, score.awayTeamValue)
        } ?: stringResource(R.string.history_result_unavailable_accessibility, home, away)
    }
    val status = when (presentation) {
        MatchBetsPresentation.HIDDEN -> stringResource(R.string.match_bets_starts_label, dateTimeText)
        MatchBetsPresentation.BETS_CLOSED -> stringResource(R.string.match_bets_closed_label)
        MatchBetsPresentation.COMPUTED -> dateTimeText
    }
    return joinSentences(listOf(matchup, status))
}

private val SECTION_SPACING = 16.dp
private val MINIMUM_GAP = 12.dp
private val FLAG_NAME_SPACING = 8.dp
private val FLAG_WIDTH = 52.dp
private val MAXIMUM_FLAG_WIDTH = 72.dp
private const val FLAG_ASPECT_RATIO = 0.75f
private val FINAL_SCORE_SIZE = 56.sp
private val MATCH_SCORE_SIZE = 40.sp
private const val SEPARATOR_SCALE = 0.6f
private const val SCORE_LINE_HEIGHT_SCALE = 1.15f
private val SCORE_SPACING = 12.dp
private val PILL_HORIZONTAL_PADDING = 14.dp
private val PILL_VERTICAL_PADDING = 6.dp

/** At or above this font scale the teams stack around the center. */
private const val STACKED_FONT_SCALE = 1.5f

@PreviewLightDark
@Preview(locale = "es-rCO", fontScale = 2f)
@Composable
private fun MatchHeaderPreview() {
    val base = historyBetPreviewModels()[1]
    TycheTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(32.dp), modifier = Modifier.padding(16.dp)) {
                MatchHeader(match = base.copy(isLocked = false, isComputed = false), presentation = MatchBetsPresentation.HIDDEN)
                MatchHeader(
                    match = base.copy(isComputed = false, matchScore = null),
                    presentation = MatchBetsPresentation.BETS_CLOSED,
                )
                MatchHeader(
                    match = base.copy(isComputed = false, matchScore = TeamScore(1, 0)),
                    presentation = MatchBetsPresentation.BETS_CLOSED,
                )
                MatchHeader(match = base, presentation = MatchBetsPresentation.COMPUTED)
                MatchHeader(match = base.copy(matchScore = null), presentation = MatchBetsPresentation.COMPUTED)
                MatchHeader(
                    match = matchHeaderPlaceholderModel(),
                    presentation = MatchBetsPresentation.HIDDEN,
                    isPlaceholder = true,
                )
            }
        }
    }
}
