package com.felipearpa.tyche.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.felipearpa.tyche.core.type.KickoffGroupProgress
import com.felipearpa.tyche.core.type.RankMovement
import com.felipearpa.tyche.ui.theme.LocalExtendedColorScheme
import com.felipearpa.tyche.ui.theme.LocalLoadingPlaceholderPulse
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.revenuecat.placeholder.placeholder
import kotlin.math.abs

/** How a [TrendIndicator] sits in its surface. */
enum class TrendIndicatorStyle {
    /** Symbol and value on the surface itself, centered, as in the leaderboard and My pools rank rails. */
    Plain,

    /**
     * A neutral pill around the symbol and value, trailing-aligned, as in History and Timeline
     * group headers.
     */
    Badge,
}

/**
 * The shared presentation of a gambler's rank movement. It owns the direction, symbol, semantic
 * color, and absolute-value formatting of a numeric movement, the `1 of 2 computed` qualifier of a
 * partial kickoff group, and the explicit pending and unavailable labels, so no surface repeats
 * those decisions. Pending and unavailable never use the steady symbol, which means a confirmed
 * zero.
 *
 * The indicator is not announced on its own: the enclosing row or header clears its children's
 * semantics and speaks [accessibilityText] as part of its single announcement. With [isPlaceholder] each leaf is masked
 * with the shared [LocalLoadingPlaceholderPulse]; the caller passes no effect.
 */
@Composable
fun TrendIndicator(
    movement: RankMovement,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall,
    style: TrendIndicatorStyle = TrendIndicatorStyle.Plain,
    isPlaceholder: Boolean = false,
) {
    val leafMask = leafMask(isPlaceholder)
    val alignment = if (style == TrendIndicatorStyle.Badge) Alignment.End else Alignment.CenterHorizontally
    Column(
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(QUALIFIER_SPACING),
        modifier = modifier,
    ) {
        SymbolLine(style = style) {
            SymbolContent(movement = movement, textStyle = textStyle, style = style, leafMask = leafMask)
        }
        partialProgress(movement)?.let { progress ->
            Label(
                text = stringResource(R.string.trend_progress_label, progress.computedMatches, progress.totalMatches),
                textStyle = qualifierTextStyle(style, textStyle),
                style = style,
                leafMask = leafMask,
            )
        }
    }
}

/**
 * Whether [TrendIndicator] shows text beyond a symbol and number: a partial group's qualifier or
 * the pending or unavailable label. Rails reserve room to wrap it.
 */
val RankMovement.hasLabel: Boolean
    get() = when (this) {
        RankMovement.Pending, RankMovement.Unavailable -> true
        is RankMovement.Moved -> partialProgress != null
    }

/**
 * The width a plain [TrendIndicator] needs so its qualifier or state label wraps only between
 * words: the widest single word of that label in [textStyle]. Zero when the movement shows no
 * label. Rails take the larger of this and their own label width.
 */
@Composable
fun RankMovement.labelWordWidth(textStyle: TextStyle): Dp {
    val label = when (this) {
        RankMovement.Pending -> stringResource(R.string.trend_pending_label)
        RankMovement.Unavailable -> stringResource(R.string.trend_unavailable_label)
        is RankMovement.Moved -> partialProgress?.let {
            stringResource(R.string.trend_progress_label, it.computedMatches, it.totalMatches)
        }
    } ?: return 0.dp
    val textMeasurer = rememberTextMeasurer()
    val widest = label.split(' ').maxOf { word ->
        textMeasurer.measure(text = word, style = textStyle.untracked(), softWrap = false).size.width
    }
    return with(LocalDensity.current) { widest.toDp() }
}

/**
 * The spoken movement for a row or header announcement: the direction with its place count in
 * full words, then the computed count of a partial group; or the pending or unavailable state.
 */
@Composable
fun RankMovement.accessibilityText(): String = when (this) {
    RankMovement.Pending -> stringResource(R.string.trend_pending_accessibility)
    RankMovement.Unavailable -> stringResource(R.string.trend_unavailable_accessibility)
    is RankMovement.Moved -> {
        val direction = directionAccessibilityText(places)
        val progress = partialProgress
        if (progress == null) {
            direction
        } else {
            val qualifier = pluralStringResource(
                R.plurals.trend_progress_accessibility,
                progress.totalMatches,
                progress.computedMatches,
                progress.totalMatches,
            )
            "$direction, $qualifier"
        }
    }
}

@Composable
private fun directionAccessibilityText(places: Int): String = when {
    places > 0 -> pluralStringResource(R.plurals.trend_up_accessibility, places, places)
    places < 0 -> pluralStringResource(R.plurals.trend_down_accessibility, abs(places), abs(places))
    else -> stringResource(R.string.trend_unchanged_accessibility)
}

@Composable
private fun SymbolLine(style: TrendIndicatorStyle, content: @Composable () -> Unit) {
    when (style) {
        TrendIndicatorStyle.Plain -> content()
        TrendIndicatorStyle.Badge -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(color = badgeFill, shape = CircleShape)
                .padding(horizontal = BADGE_HORIZONTAL_PADDING, vertical = BADGE_VERTICAL_PADDING),
        ) {
            content()
        }
    }
}

@Composable
private fun SymbolContent(
    movement: RankMovement,
    textStyle: TextStyle,
    style: TrendIndicatorStyle,
    leafMask: Modifier,
) {
    val colors = LocalExtendedColorScheme.current
    when (movement) {
        is RankMovement.Moved -> when {
            movement.places > 0 -> DirectionalTrend(
                iconId = R.drawable.arrow_upward,
                places = movement.places,
                color = colors.gain,
                textStyle = textStyle,
                style = style,
                leafMask = leafMask,
                testTag = "upProgressIndicator",
            )

            movement.places < 0 -> DirectionalTrend(
                iconId = R.drawable.arrow_downward,
                places = movement.places,
                color = colors.drop,
                textStyle = textStyle,
                style = style,
                leafMask = leafMask,
                testTag = "downProgressIndicator",
            )

            else -> Icon(
                painter = painterResource(id = R.drawable.horizontal_rule),
                contentDescription = null,
                tint = colors.steady,
                modifier = leafMask
                    .size(iconSizeFor(textStyle))
                    .testTag("stableProgressIndicator"),
            )
        }

        RankMovement.Pending -> Label(
            text = stringResource(R.string.trend_pending_label),
            textStyle = textStyle,
            style = style,
            leafMask = leafMask,
            modifier = Modifier.testTag("pendingTrendIndicator"),
        )

        RankMovement.Unavailable -> Label(
            text = stringResource(R.string.trend_unavailable_label),
            textStyle = textStyle,
            style = style,
            leafMask = leafMask,
            modifier = Modifier.testTag("unavailableTrendIndicator"),
        )
    }
}

/** An arrow with the absolute number of places moved. */
@Composable
private fun DirectionalTrend(
    iconId: Int,
    places: Int,
    color: Color,
    textStyle: TextStyle,
    style: TrendIndicatorStyle,
    leafMask: Modifier,
    testTag: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.testTag(testTag),
    ) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = null,
            tint = color,
            modifier = leafMask.size(iconSizeFor(textStyle)),
        )
        if (style == TrendIndicatorStyle.Badge) {
            Spacer(modifier = Modifier.width(BADGE_SYMBOL_SPACING))
        }
        Text(
            text = abs(places).toString(),
            style = textStyle.copy(fontFeatureSettings = TABULAR_NUMBERS),
            fontWeight = if (style == TrendIndicatorStyle.Badge) FontWeight.SemiBold else null,
            color = color,
            maxLines = 1,
            modifier = leafMask,
        )
    }
}

/** A qualifier or state label; it wraps on word boundaries within the width its caller gives it. */
@Composable
private fun Label(
    text: String,
    textStyle: TextStyle,
    style: TrendIndicatorStyle,
    leafMask: Modifier,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = textStyle.untracked(),
        color = secondaryText,
        textAlign = if (style == TrendIndicatorStyle.Badge) TextAlign.End else TextAlign.Center,
        modifier = modifier.then(leafMask),
    )
}

/** Labels drop the style's tracking: spaced-out words read poorly and need more room to wrap whole. */
private fun TextStyle.untracked(): TextStyle = copy(letterSpacing = 0.sp)

private fun partialProgress(movement: RankMovement): KickoffGroupProgress? =
    (movement as? RankMovement.Moved)?.partialProgress

/** The qualifier stays one step below the badge's value; on a plain rail it matches it. */
@Composable
@ReadOnlyComposable
private fun qualifierTextStyle(style: TrendIndicatorStyle, textStyle: TextStyle): TextStyle =
    if (style == TrendIndicatorStyle.Badge) MaterialTheme.typography.bodySmall else textStyle

@Composable
private fun leafMask(isPlaceholder: Boolean): Modifier {
    if (!isPlaceholder) return Modifier
    val pulse = LocalLoadingPlaceholderPulse.current
    return Modifier.placeholder(color = pulse.color, shape = pulse.shape, highlight = pulse.highlight)
}

@Composable
private fun iconSizeFor(textStyle: TextStyle): Dp =
    with(LocalDensity.current) { textStyle.fontSize.toDp() * ICON_SCALE }

/**
 * Qualifier and state labels: History's secondary text, at least 5.4:1 on every surface the
 * indicator appears on (see `TrendColorContrastTest`).
 */
private val secondaryText: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY_TEXT_ALPHA)

/** The badge's neutral fill; trend colors keep at least 4.5:1 on it in both themes. */
private val badgeFill: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceVariant

// Icon sized proportionally to the text (~line height), e.g. 16.5.dp at 11.sp labelSmall.
private const val ICON_SCALE = 1.5f
private const val SECONDARY_TEXT_ALPHA = 0.7f
private const val TABULAR_NUMBERS = "tnum"
private val QUALIFIER_SPACING = 2.dp
private val BADGE_HORIZONTAL_PADDING = 12.dp
private val BADGE_VERTICAL_PADDING = 6.dp
private val BADGE_SYMBOL_SPACING = 4.dp

private val previewMovements = listOf(
    RankMovement.Moved(places = 3, partialProgress = null),
    RankMovement.Moved(places = -1, partialProgress = null),
    RankMovement.Moved(places = 0, partialProgress = null),
    RankMovement.Moved(places = 3, partialProgress = KickoffGroupProgress(computedMatches = 1, totalMatches = 2)),
    RankMovement.Pending,
    RankMovement.Unavailable,
)

@PreviewLightDark
@Preview(locale = "es-rCO", fontScale = 2f)
@Composable
private fun PlainTrendIndicatorPreview() {
    TycheTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(16.dp)) {
                previewMovements.forEach { movement ->
                    TrendIndicator(movement = movement, modifier = Modifier.width(64.dp))
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun BadgeTrendIndicatorPreview() {
    TycheTheme {
        Surface {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                previewMovements.forEach { movement ->
                    TrendIndicator(
                        movement = movement,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        style = TrendIndicatorStyle.Badge,
                    )
                }
            }
        }
    }
}
