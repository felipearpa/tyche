package com.felipearpa.tyche.bet.finished

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.ui.theme.LocalExtendedColorScheme
import com.felipearpa.tyche.ui.theme.LocalLoadingPlaceholderPulse
import com.revenuecat.placeholder.placeholder

/**
 * The capsule that shows a points value or status in History, Timeline, and the match screen's
 * participant rows. A positive award uses the positive colors; everything else is neutral. Inside
 * the signed-in gambler's highlighted row the colors switch to the current-user roles, so the pill
 * stays distinct from the row's own fill.
 *
 * A placeholder masks the whole pill once, rather than its fill and its text separately. The pill
 * adds no semantics of its own; its row announces the value.
 */
@Composable
internal fun HistoryPointsPill(
    text: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier,
    surface: HistoryPointsPillSurface = HistoryPointsPillSurface.CANVAS,
    /** When set, the pill is at least as wide as this text, so a column of pills lines up. */
    reservedText: String? = null,
    /** A numeric value stays on one line; status words may wrap at large font scales. */
    maxLines: Int = 1,
    isPlaceholder: Boolean = false,
) {
    val fill = if (isPlaceholder) Modifier else Modifier.background(color = pillFill(surface, isPositive), shape = CircleShape)
    val mask = if (isPlaceholder) {
        val pulse = LocalLoadingPlaceholderPulse.current
        Modifier.placeholder(color = pulse.color, shape = CircleShape, highlight = pulse.highlight)
    } else {
        Modifier
    }
    val style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum")

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .then(mask)
            .then(fill)
            .padding(horizontal = PILL_HORIZONTAL_PADDING, vertical = PILL_VERTICAL_PADDING),
    ) {
        if (reservedText != null) {
            Text(
                text = reservedText,
                style = style,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier
                    .alpha(0f)
                    .clearAndSetSemantics {},
            )
        }
        Text(
            text = text,
            style = style,
            fontWeight = FontWeight.SemiBold,
            color = pillForeground(surface, isPositive),
            maxLines = maxLines,
        )
    }
}

/** The surface a [HistoryPointsPill] sits on. */
internal enum class HistoryPointsPillSurface {
    /** The inherited screen canvas. */
    CANVAS,

    /** The signed-in gambler's `currentUserContainer` row. */
    CURRENT_USER_ROW,
}

@Composable
private fun pillForeground(surface: HistoryPointsPillSurface, isPositive: Boolean): Color {
    val extendedColors = LocalExtendedColorScheme.current
    return when (surface) {
        HistoryPointsPillSurface.CANVAS ->
            if (isPositive) HistoryStyle.positiveText else HistoryStyle.secondaryText

        HistoryPointsPillSurface.CURRENT_USER_ROW ->
            if (isPositive) extendedColors.onCurrentUser else extendedColors.onCurrentUserContainer
    }
}

@Composable
private fun pillFill(surface: HistoryPointsPillSurface, isPositive: Boolean): Color {
    val extendedColors = LocalExtendedColorScheme.current
    return when (surface) {
        HistoryPointsPillSurface.CANVAS -> if (isPositive) HistoryStyle.positiveFill else HistoryStyle.neutralFill
        // The row's own tint keeps the neutral text at 4.5:1 or more on the light current-user row.
        HistoryPointsPillSurface.CURRENT_USER_ROW ->
            if (isPositive) {
                extendedColors.currentUser
            } else {
                extendedColors.onCurrentUserContainer.copy(alpha = HistoryStyle.NEUTRAL_FILL_ALPHA)
            }
    }
}

private val PILL_HORIZONTAL_PADDING = 14.dp
private val PILL_VERTICAL_PADDING = 6.dp
