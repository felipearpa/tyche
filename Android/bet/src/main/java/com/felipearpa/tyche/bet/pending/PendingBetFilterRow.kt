package com.felipearpa.tyche.bet.pending

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ripple
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.bet.R
import com.felipearpa.tyche.ui.theme.TycheTheme
import java.text.NumberFormat
import java.util.Locale

/**
 * The All / No bet / Placed chips with their counts. The selected chip takes the app's selection
 * pair, `primaryContainer` with `onPrimaryContainer` text; the others keep their outline. While
 * more pages may load ([isPartial]) each count shows a trailing "+" and TalkBack reads it as "at
 * least N matches". A changed count swaps without overlap while the chip's width eases. When the
 * chips do not fit (large text), the row scrolls horizontally.
 */
@Composable
internal fun PendingBetFilterRow(
    selected: PendingBetFilter,
    counts: PendingBetFilterCounts,
    isPartial: Boolean,
    onSelect: (PendingBetFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.horizontalScroll(rememberScrollState()),
    ) {
        PendingBetFilter.entries.forEach { filter ->
            PendingBetFilterChip(
                title = filter.title,
                count = counts.count(filter),
                isPartial = isPartial,
                isSelected = filter == selected,
                onClick = { onSelect(filter) },
            )
        }
    }
}

@Composable
private fun PendingBetFilterChip(
    @StringRes title: Int,
    count: Int,
    isPartial: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = pendingBetColors()
    val reduceMotion = reduceMotion()
    val titleText = stringResource(title)
    val formattedCount = formatFilterCount(count, LocalLocale.current.platformLocale)
    val countLabel = if (isPartial) stringResource(R.string.bets_filter_count_partial, formattedCount) else formattedCount
    val spokenCount = if (isPartial) {
        pluralStringResource(R.plurals.bets_filter_count_at_least_accessibility, count, formattedCount)
    } else {
        pluralStringResource(R.plurals.history_group_match_count, count, count)
    }

    val fill by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = PendingBetStyle.spec(),
        label = "filterChipFill",
    )
    val label by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else colors.primaryText,
        animationSpec = PendingBetStyle.spec(),
        label = "filterChipLabel",
    )
    val outline by animateColorAsState(
        targetValue = if (isSelected) Color.Transparent else colors.emptyOutline,
        animationSpec = PendingBetStyle.spec(),
        label = "filterChipOutline",
    )
    val interactionSource = remember { MutableInteractionSource() }

    // The target is at least 48 dp tall around the 36-dp capsule, which shows the ripple.
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .sizeIn(minHeight = MINIMUM_TARGET)
            .selectable(
                selected = isSelected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics { contentDescription = "$titleText, $spokenCount" },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(PendingBetStyle.chipHeight)
                .clip(CircleShape)
                .background(fill)
                .border(1.dp, outline, CircleShape)
                .indication(interactionSource, ripple())
                .padding(horizontal = 16.dp)
                .clearAndSetSemantics {},
        ) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = label,
                maxLines = 1,
            )
            AnimatedContent(
                targetState = countLabel,
                transitionSpec = { PendingBetStyle.contentSwap(reduceMotion) },
                contentAlignment = Alignment.CenterStart,
                label = "filterCount",
            ) { shownLabel ->
                Text(
                    text = shownLabel,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = PendingBetStyle.TABULAR_FIGURES),
                    color = label,
                    maxLines = 1,
                    modifier = Modifier.alpha(COUNT_ALPHA),
                )
            }
        }
    }
}

private val MINIMUM_TARGET = 48.dp

private const val COUNT_ALPHA = 0.7f

/** A filter count in [locale]'s number format, grouping separator included ("1,123", "1.123"). */
internal fun formatFilterCount(count: Int, locale: Locale): String = NumberFormat.getIntegerInstance(locale).format(count)

@get:StringRes
private val PendingBetFilter.title: Int
    get() = when (this) {
        PendingBetFilter.ALL -> R.string.bets_filter_all
        PendingBetFilter.NO_BET -> R.string.bets_filter_no_bet
        PendingBetFilter.PLACED -> R.string.bets_filter_placed
    }

/**
 * The message shown when the filter keeps no loaded match. All keeps every match, so it has none:
 * an empty pool shows the list's own empty state.
 */
@get:StringRes
internal val PendingBetFilter.emptyMessage: Int?
    get() = when (this) {
        PendingBetFilter.ALL -> null
        PendingBetFilter.NO_BET -> R.string.bets_filter_empty_message
        PendingBetFilter.PLACED -> R.string.bets_filter_placed_empty_message
    }

private fun PendingBetFilterCounts.count(filter: PendingBetFilter): Int = when (filter) {
    PendingBetFilter.ALL -> all
    PendingBetFilter.NO_BET -> noBet
    PendingBetFilter.PLACED -> placed
}

@PreviewLightDark
@Composable
private fun PendingBetFilterRowPreview() {
    TycheTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(16.dp)) {
                PendingBetFilterRow(
                    selected = PendingBetFilter.ALL,
                    counts = PendingBetFilterCounts(all = 6, noBet = 3, placed = 3),
                    isPartial = false,
                    onSelect = {},
                )
                PendingBetFilterRow(
                    selected = PendingBetFilter.NO_BET,
                    counts = PendingBetFilterCounts(all = 6, noBet = 2, placed = 4),
                    isPartial = true,
                    onSelect = {},
                )
            }
        }
    }
}
