package com.felipearpa.tyche.bet.kickoffgroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.bet.R
import com.felipearpa.tyche.bet.finished.HistoryMatchDateFormat
import com.felipearpa.tyche.bet.finished.HistoryStyle
import com.felipearpa.tyche.bet.finished.joinSentences
import com.felipearpa.tyche.bet.finished.rememberHistoryMatchDateFormat
import com.felipearpa.tyche.ui.TrendIndicator
import com.felipearpa.tyche.ui.TrendIndicatorStyle
import com.felipearpa.tyche.ui.accessibilityText
import com.felipearpa.tyche.ui.theme.LocalLoadingPlaceholderPulse
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.revenuecat.placeholder.placeholder

/**
 * The scrolling header of one kickoff group in History and Timeline: the localized date, then the
 * time with the group's total match count, and the group's movement badge at the trailing edge. A
 * partial group's computed count sits under the badge. The header is not an action and is
 * announced once, as a heading, with the date and time, the total count, and the movement. A
 * one-match group omits the visible count.
 *
 * Metadata and badge share a line; when a long date or large text leaves no room, the badge moves
 * below the metadata instead of clipping either.
 *
 * Loading slots render this same component from [kickoffGroupPlaceholderModel] with
 * [isPlaceholder] set: its leaves are masked with the shared [LocalLoadingPlaceholderPulse] and it
 * has no semantics.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun KickoffGroupHeader(
    group: KickoffGroupModel,
    dateFormat: HistoryMatchDateFormat,
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
) {
    val dateText = dateFormat.date(group.kickoffDateTime)
    val timeText = dateFormat.time(group.kickoffDateTime)
    val matchCountText =
        pluralStringResource(R.plurals.history_group_match_count, group.totalMatches, group.totalMatches)
    val announcement =
        joinSentences(listOf("$dateText, $timeText", matchCountText, group.movement.accessibilityText()))
    val leafMask = leafMask(isPlaceholder)

    FlowRow(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(STACKED_SPACING),
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                if (!isPlaceholder) {
                    contentDescription = announcement
                    heading()
                }
            },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(METADATA_LINE_SPACING),
            modifier = Modifier.padding(end = MINIMUM_GAP),
        ) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = HistoryStyle.primaryText,
                modifier = leafMask,
            )
            Text(
                text = if (group.totalMatches > 1) "$timeText · $matchCountText" else timeText,
                style = MaterialTheme.typography.bodyMedium,
                color = HistoryStyle.secondaryText,
                modifier = leafMask,
            )
        }
        TrendIndicator(
            movement = group.movement,
            textStyle = MaterialTheme.typography.bodyLarge,
            style = TrendIndicatorStyle.Badge,
            isPlaceholder = isPlaceholder,
        )
    }
}

@Composable
private fun leafMask(isPlaceholder: Boolean): Modifier {
    if (!isPlaceholder) return Modifier
    val pulse = LocalLoadingPlaceholderPulse.current
    return Modifier.placeholder(color = pulse.color, shape = pulse.shape, highlight = pulse.highlight)
}

private val MINIMUM_GAP = 12.dp
private val METADATA_LINE_SPACING = 2.dp
private val STACKED_SPACING = 8.dp

@PreviewLightDark
@Preview(name = "Large text, Spanish", locale = "es-rCO", fontScale = 2f, widthDp = 320)
@Composable
private fun KickoffGroupHeaderPreview() {
    val dateFormat = rememberHistoryMatchDateFormat()
    TycheTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(horizontal = HistoryStyle.horizontalPadding, vertical = 16.dp),
            ) {
                timelineGroupPreviewModels().forEach { group ->
                    KickoffGroupHeader(group = group, dateFormat = dateFormat)
                }
                KickoffGroupHeader(
                    group = kickoffGroupPlaceholderModel(),
                    dateFormat = dateFormat,
                    isPlaceholder = true,
                )
            }
        }
    }
}
