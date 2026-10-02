package com.felipearpa.tyche.ui.theme

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.revenuecat.placeholder.PlaceholderHighlight
import com.revenuecat.placeholder.Pulse

/**
 * Fortuna's shared loading-placeholder pulse: the fill, highlight, timing, and default shape that
 * every production component reads when it renders itself with `isPlaceholder = true`.
 *
 * Components own their placeholder rendering and pass these values straight to RevenueCat's
 * `Modifier.placeholder` on each content leaf (text, avatar, score, indicator); screens never
 * pass an effect. Structural fills such as row canvases and rank tiles stay outside the mask.
 *
 * RevenueCat hides the leaf's content, draws [color], and draws [highlight] over it at an alpha
 * that follows the animation progress. The values are calibrated to match the iOS
 * native-redaction pulse. Light appearance uses the same composite as iOS: black at
 * 9.6% → 16% → 9.6% over white. Dark appearance matches iOS in perceived lightness (CIELAB L*)
 * against each platform's own canvas rather than in alpha. iOS draws white at 10.7% → 16% over
 * black (ΔL* 9.9 → 16.5); over Android's #121212 surface the same ΔL* needs white at
 * 8.6% → 14.6%. With system animations off there is no highlight and [color] is the static
 * midpoint (12.8% light; 11.6% dark, ΔL* 13.2 like the iOS 13.3% midpoint).
 */
@Immutable
data class LoadingPlaceholderPulse(
    /** Fill at the dimmest point of the cycle, or the static midpoint when [highlight] is null. */
    val color: Color,
    /** The pulse drawn over [color]; null when system animations are disabled. */
    val highlight: PlaceholderHighlight?,
    /** Default mask shape for text and small glyphs; avatars use their own circular shape. */
    val shape: Shape = RoundedCornerShape(TEXT_CORNER_RADIUS),
) {
    companion object {
        /** Each fade direction at normal speed; the full cycle is two legs with no pause. */
        const val LEG_DURATION_MILLIS = 900
        const val CYCLE_DURATION_MILLIS = LEG_DURATION_MILLIS * 2

        /**
         * Progress 0 → 1 → 0 over one cycle, symmetric ease-in-out, no delay. Compose scales it
         * by the system animator duration scale through the composition's [MotionDurationScale].
         */
        val animationSpec = infiniteRepeatable<Float>(
            animation = tween(durationMillis = LEG_DURATION_MILLIS, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        )

        /** Composite black fill: 9.6% dim, 16% bright, 12.8% static. */
        val light = Appearance(
            dim = Color.Black.copy(alpha = 0.096f),
            // 1 - (1 - 0.096)(1 - h) = 0.16
            highlight = Color.Black.copy(alpha = 0.0708f),
            static = Color.Black.copy(alpha = 0.128f),
        )

        /**
         * Composite white fill over #121212: 8.6% dim, 14.6% bright, 11.6% static. These give
         * the same ΔL* against the dark surface as iOS's 10.7% / 16% / 13.3% over black.
         */
        val dark = Appearance(
            dim = Color.White.copy(alpha = 0.086f),
            // 1 - (1 - 0.086)(1 - h) = 0.146
            highlight = Color.White.copy(alpha = 0.0656f),
            static = Color.White.copy(alpha = 0.116f),
        )

        fun of(darkTheme: Boolean, animationsEnabled: Boolean): LoadingPlaceholderPulse {
            val appearance = if (darkTheme) dark else light
            return if (animationsEnabled) {
                LoadingPlaceholderPulse(
                    color = appearance.dim,
                    highlight = Pulse(
                        highlightColor = appearance.highlight,
                        animationSpec = animationSpec,
                    ),
                )
            } else {
                LoadingPlaceholderPulse(color = appearance.static, highlight = null)
            }
        }
    }

    @Immutable
    data class Appearance(val dim: Color, val highlight: Color, val static: Color)
}

val LocalLoadingPlaceholderPulse = compositionLocalOf {
    LoadingPlaceholderPulse.of(darkTheme = false, animationsEnabled = true)
}

/**
 * Whether system animations are on, read from the composition's [MotionDurationScale]. On
 * Android the window recomposer backs it with snapshot state that follows
 * `Settings.Global.ANIMATOR_DURATION_SCALE`, so a preference change recomposes the readers.
 */
@Composable
internal fun systemAnimationsEnabled(): Boolean {
    val scale = rememberCoroutineScope().coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
    return scale > 0f
}

private val TEXT_CORNER_RADIUS = 4.dp
