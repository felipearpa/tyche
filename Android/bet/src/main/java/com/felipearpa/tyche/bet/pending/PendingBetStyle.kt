package com.felipearpa.tyche.bet.pending

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.ui.theme.LocalExtendedColorScheme
import com.felipearpa.tyche.ui.theme.systemAnimationsEnabled

/**
 * Measurements and motion of the Bets tab's match cards, shared with the iOS card.
 *
 * Every card change runs on one curve, the iOS card's ease-in-out over [ANIMATION_MILLIS]: the
 * status pill's crossfade and width, score box and filter chip colors, rows entering or leaving
 * below the scores, and the cards that move with them. Content that swaps in place (the pill,
 * the filter counts) never overlaps: the old content eases out within the first 40 % of the
 * change and the new one fades in over the rest, as on iOS.
 */
internal object PendingBetStyle {
    const val ANIMATION_MILLIS = 250
    private const val CONTENT_FADE_OUT_MILLIS = ANIMATION_MILLIS * 4 / 10
    private const val CONTENT_FADE_IN_MILLIS = ANIMATION_MILLIS - CONTENT_FADE_OUT_MILLIS

    val cardCornerRadius = 16.dp
    val cardHorizontalPadding = 16.dp
    val cardVerticalPadding = 14.dp
    val cardSpacing = 12.dp
    val flagSize = 44.dp
    val scoreBoxSize = 52.dp
    val scoreBoxCornerRadius = 12.dp
    val stepperWidth = 52.dp
    val stepperHeight = 44.dp
    val stepperGlyphSize = 24.dp
    val chipHeight = 36.dp
    val pillHeight = 26.dp
    val pillHorizontalPadding = 10.dp
    val pillGlyphSize = 14.dp
    val spinnerSize = 20.dp
    val outlineWidth = 1.5.dp
    val focusedOutlineWidth = 2.dp

    /** The score digits keep one size whatever the font scale, as the box around them does. */
    @Composable
    fun scoreTextStyle(): TextStyle = with(LocalDensity.current) {
        TextStyle(fontSize = SCORE_DIGIT_SIZE.toSp(), fontWeight = FontWeight.Bold, fontFeatureSettings = TABULAR_FIGURES)
    }

    const val TABULAR_FIGURES = "tnum"
    private val SCORE_DIGIT_SIZE: Dp = 34.dp

    fun <T> spec(): FiniteAnimationSpec<T> = tween(durationMillis = ANIMATION_MILLIS, easing = EaseInOut)

    /** Swaps one piece of content for another without overlap; with [reduceMotion] the size snaps. */
    fun contentSwap(reduceMotion: Boolean): ContentTransform = ContentTransform(
        targetContentEnter = fadeIn(tween(CONTENT_FADE_IN_MILLIS, delayMillis = CONTENT_FADE_OUT_MILLIS, easing = EaseInOut)),
        initialContentExit = fadeOut(tween(CONTENT_FADE_OUT_MILLIS, easing = EaseOut)),
        sizeTransform = SizeTransform(clip = false) { _, _ -> if (reduceMotion) snap() else spec() },
    )

    /** Rows appearing below the scores: the card grows while they fade in; a fade only with [reduceMotion]. */
    fun enterBelow(reduceMotion: Boolean): EnterTransition =
        if (reduceMotion) fadeIn(spec()) else expandVertically(spec()) + fadeIn(spec())

    fun exitBelow(reduceMotion: Boolean): ExitTransition =
        if (reduceMotion) fadeOut(spec()) else shrinkVertically(spec()) + fadeOut(spec())
}

/**
 * Android has no Reduce Motion switch of its own; "Remove animations" (animator scale 0) is the
 * signal. Movement then falls back to fades and size changes snap.
 */
@Composable
internal fun reduceMotion(): Boolean = !systemAnimationsEnabled()

/**
 * The card's colors in the current theme.
 *
 * Measured contrast (WCAG) on the card (`surfaceVariant`, `#F5F5F5` light and `#1E1E1E` dark),
 * light / dark:
 * - [secondaryText] (onSurface at 70 %): 5.7:1 / 6.9:1; on the screen surface (day headers): 5.9:1 / 7.3:1.
 * - [amber] text (`#8C5E00` / `#FFC107`): 5.2:1 / 10.2:1 on the card, 4.9:1 / 7.5:1 on [amberFill].
 * - [emptyOutline], the dashed boundary of an empty box and the chip outlines: 3.1:1 / 4.2:1.
 * - [focusedOutline], the primary green: 4.7:1 / 6.0:1.
 * - [errorText] (`#B71C1C` / `#EF9A9A`) on [errorFill]: 4.9:1 / 6.5:1; Retry's [onRetry] on [retryFill]: 5.0:1 / 4.6:1.
 * - [savedText] (`#176B3A` / `#7EDB9A`) on [savedFill]: 4.9:1 / 7.7:1.
 * - [secondaryText] on [controlFill] (Locked and Saving pills): 5.2:1 / 5.7:1.
 * - [secondaryText] stepper glyphs on [stepperFill]: 5.4:1 / 5.8:1.
 */
@Immutable
internal data class PendingBetColors(
    val cardFill: Color,
    val controlFill: Color,
    /** The `+`/`−` steppers' fill, softer than the score boxes: 1.14:1 against the light card and 1.25:1 against the dark one. */
    val stepperFill: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val emptyOutline: Color,
    val focusedOutline: Color,
    val amber: Color,
    val amberFill: Color,
    val savedText: Color,
    val savedFill: Color,
    val errorText: Color,
    val errorFill: Color,
    val retryFill: Color,
    val onRetry: Color,
    val headerBackground: Color,
)

@Composable
internal fun pendingBetColors(): PendingBetColors {
    val scheme = MaterialTheme.colorScheme
    val extended = LocalExtendedColorScheme.current
    val isDark = scheme.surface.luminance() < 0.5f
    return PendingBetColors(
        cardFill = scheme.surfaceVariant,
        controlFill = scheme.onSurface.copy(alpha = CONTROL_FILL_ALPHA),
        stepperFill = scheme.onSurface.copy(alpha = if (isDark) DARK_STEPPER_FILL_ALPHA else LIGHT_STEPPER_FILL_ALPHA),
        primaryText = scheme.onSurface,
        secondaryText = scheme.onSurface.copy(alpha = 0.7f),
        emptyOutline = scheme.onSurface.copy(alpha = 0.5f),
        focusedOutline = scheme.primary,
        amber = if (isDark) AMBER else DARK_AMBER,
        amberFill = AMBER.copy(alpha = 0.14f),
        savedText = extended.currentUser,
        savedFill = scheme.primary.copy(alpha = 0.16f),
        errorText = extended.drop,
        errorFill = scheme.error.copy(alpha = 0.14f),
        retryFill = scheme.error,
        onRetry = scheme.onError,
        headerBackground = scheme.surface,
    )
}

private const val CONTROL_FILL_ALPHA = 0.10f
private const val LIGHT_STEPPER_FILL_ALPHA = 0.07f
private const val DARK_STEPPER_FILL_ALPHA = 0.09f

/** The theme's secondary amber. */
private val AMBER = Color(0xFFFFC107)

/** Amber dark enough for text on the light card. */
private val DARK_AMBER = Color(0xFF8C5E00)
