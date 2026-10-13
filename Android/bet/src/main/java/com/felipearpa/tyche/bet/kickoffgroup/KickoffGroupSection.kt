package com.felipearpa.tyche.bet.kickoffgroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.finished.HistoryBetItem
import com.felipearpa.tyche.bet.finished.HistoryMatchDateFormat
import com.felipearpa.tyche.bet.finished.HistoryOwner
import com.felipearpa.tyche.bet.finished.HistoryStyle
import com.felipearpa.tyche.bet.finished.rememberHistoryMatchDateFormat
import com.felipearpa.tyche.ui.theme.TycheTheme

/**
 * One kickoff group of History or Timeline: its header, then its match rows joined by a subdued
 * vertical rail and separated by thin dividers. Each row keeps its own match action and press
 * feedback; the header and rail are not actions, and the rail adds no semantics. [owner] selects
 * the rows' ownership wording.
 *
 * Loading slots render this same component from [kickoffGroupPlaceholderModel] with
 * [isPlaceholder] set; the header and rows then render as placeholders and open nothing.
 */
@Composable
internal fun KickoffGroupSection(
    group: KickoffGroupModel,
    dateFormat: HistoryMatchDateFormat,
    modifier: Modifier = Modifier,
    owner: HistoryOwner = HistoryOwner.SignedInGambler,
    isPlaceholder: Boolean = false,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)? = null,
) {
    val railFill = HistoryStyle.railFill
    Column(
        verticalArrangement = Arrangement.spacedBy(HEADER_SPACING),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HistoryStyle.horizontalPadding)
            .padding(top = GROUP_TOP_PADDING, bottom = GROUP_BOTTOM_PADDING),
    ) {
        KickoffGroupHeader(group = group, dateFormat = dateFormat, isPlaceholder = isPlaceholder)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Decorative: drawn behind the rows, so it adds no node of its own.
                .drawBehind {
                    val width = RAIL_WIDTH.toPx()
                    val inset = RAIL_INSET.toPx()
                    drawRoundRect(
                        color = railFill,
                        topLeft = Offset(x = 0f, y = inset),
                        size = Size(width = width, height = (size.height - 2 * inset).coerceAtLeast(0f)),
                        cornerRadius = CornerRadius(width / 2),
                    )
                }
                .padding(start = RAIL_WIDTH + RAIL_GAP),
        ) {
            group.matches.forEachIndexed { index, match ->
                key(match.matchId) {
                    if (index > 0) HorizontalDivider()
                    HistoryBetItem(
                        poolGamblerBet = match,
                        kickoffDateTime = group.kickoffDateTime,
                        dateFormat = dateFormat,
                        modifier = Modifier.fillMaxWidth(),
                        owner = owner,
                        isPlaceholder = isPlaceholder,
                        onClick = onMatchOpen?.takeUnless { isPlaceholder }?.let { open -> { open(match) } },
                    )
                }
            }
        }
    }
}

/** [count] production group sections from placeholder models, for an initial load. */
internal fun LazyListScope.kickoffGroupPlaceholderList(
    count: Int,
    dateFormat: HistoryMatchDateFormat,
    owner: HistoryOwner = HistoryOwner.SignedInGambler,
) {
    items(count = count, contentType = { KICKOFF_GROUP_CONTENT_TYPE }) {
        KickoffGroupPlaceholderSection(dateFormat = dateFormat, owner = owner)
    }
}

/** One production group section filled with placeholder data. It has no action. */
@Composable
internal fun KickoffGroupPlaceholderSection(
    dateFormat: HistoryMatchDateFormat,
    owner: HistoryOwner = HistoryOwner.SignedInGambler,
) {
    KickoffGroupSection(
        group = kickoffGroupPlaceholderModel(),
        dateFormat = dateFormat,
        owner = owner,
        isPlaceholder = true,
    )
}

/** Groups in History and Timeline lists share one lazy content type. */
internal const val KICKOFF_GROUP_CONTENT_TYPE = "KickoffGroup"

private val HEADER_SPACING = 4.dp
private val GROUP_TOP_PADDING = 8.dp
private val GROUP_BOTTOM_PADDING = 16.dp
private val RAIL_WIDTH = 2.dp
private val RAIL_GAP = 16.dp
private val RAIL_INSET = 8.dp

@PreviewLightDark
@Composable
private fun KickoffGroupSectionSizesPreview() {
    val dateFormat = rememberHistoryMatchDateFormat()
    TycheTheme {
        Surface {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(groupSizePreviewModels(), key = { it.key }) { group ->
                    KickoffGroupSection(group = group, dateFormat = dateFormat, onMatchOpen = {})
                }
            }
        }
    }
}

@PreviewLightDark
@Preview(name = "Large text, Spanish", locale = "es-rCO", fontScale = 2f, widthDp = 320)
@Composable
private fun KickoffGroupSectionTimelinePreview() {
    val dateFormat = rememberHistoryMatchDateFormat()
    TycheTheme {
        Surface {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(timelineGroupPreviewModels().take(3), key = { it.key }) { group ->
                    KickoffGroupSection(
                        group = group,
                        dateFormat = dateFormat,
                        owner = HistoryOwner.SelectedGambler(name = "El mono"),
                        onMatchOpen = {},
                    )
                }
                kickoffGroupPlaceholderList(count = 1, dateFormat = dateFormat)
            }
        }
    }
}
