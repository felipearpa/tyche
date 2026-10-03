package com.felipearpa.tyche.bet.finished

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.bet.R
import com.felipearpa.tyche.ui.theme.LocalExtendedColorScheme

/**
 * Colors shared by History's summary and rows. Positive points use the current-user tokens;
 * zero and unavailable values stay neutral.
 */
internal object HistoryStyle {
    val primaryText: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurface

    val secondaryText: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY_TEXT_ALPHA)

    val positiveText: Color
        @Composable @ReadOnlyComposable
        get() = LocalExtendedColorScheme.current.currentUser

    val positiveFill: Color
        @Composable @ReadOnlyComposable
        get() = LocalExtendedColorScheme.current.currentUserContainer

    val neutralFill: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurface.copy(alpha = NEUTRAL_FILL_ALPHA)

    /** Rows span the full content width on every window size, inside this gutter. */
    val horizontalPadding = 16.dp

    const val SECONDARY_TEXT_ALPHA = 0.7f
    const val NEUTRAL_FILL_ALPHA = 0.12f
}

/**
 * The visible points text: a signed value for positive awards, a plain value for zero or
 * negative ones, and a neutral dash when the value is unavailable. It never shows a bare plus.
 */
@Composable
internal fun historyPointsShortText(points: HistoryPoints): String = when {
    points !is HistoryPoints.Earned -> stringResource(R.string.history_points_unavailable_short)
    points.value > 0 ->
        pluralStringResource(R.plurals.history_positive_points_short, points.value, points.value)

    else -> pluralStringResource(R.plurals.history_points_short, points.value, points.value)
}
