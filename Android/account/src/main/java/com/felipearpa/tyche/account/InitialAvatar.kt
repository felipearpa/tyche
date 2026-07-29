package com.felipearpa.tyche.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.pow
import com.felipearpa.tyche.ui.R as SharedR

/**
 * Renders a stable, high-contrast initial avatar for any account identity.
 *
 * [identity] controls the visible initial while [colorKey] controls the
 * deterministic generated color. Callers may supply a semantic color pair for
 * states such as the signed-in leaderboard row.
 */
@Composable
fun InitialAvatar(
    identity: String,
    modifier: Modifier = Modifier,
    colorKey: String = identity,
    backgroundColor: Color? = null,
    foregroundColor: Color? = null,
) {
    val initial = identity.avatarInitial()
    val generatedColors = if (initial != null && (backgroundColor == null || foregroundColor == null)) {
        avatarColorsFor(key = colorKey, isDark = isSystemInDarkTheme())
    } else {
        null
    }
    val resolvedBackground = backgroundColor ?: generatedColors?.first ?: Color.Transparent
    val resolvedForeground = foregroundColor
        ?: generatedColors?.second
        ?: MaterialTheme.colorScheme.onSurfaceVariant

    BoxWithConstraints(
        modifier = modifier.background(resolvedBackground),
        contentAlignment = Alignment.Center,
    ) {
        val diameter = minOf(maxWidth, maxHeight)
        if (initial == null) {
            Icon(
                painter = painterResource(id = SharedR.drawable.filled_person),
                contentDescription = null,
                tint = resolvedForeground,
                modifier = Modifier.size(diameter * FALLBACK_ICON_RATIO),
            )
        } else {
            val fontSize = with(LocalDensity.current) {
                (diameter * INITIAL_FONT_RATIO).toSp()
            }
            Text(
                text = initial.toString(),
                color = resolvedForeground,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

internal fun String.avatarInitial(): Char? =
    firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()

internal fun avatarColorsFor(key: String, isDark: Boolean): Pair<Color, Color> {
    val hue = ((key.hashCode() and 0x7fffffff) % 360).toFloat()
    val foreground = if (isDark) Color.Black else Color.White
    val background = mostColorfulBackground(
        hue = hue,
        saturation = AVATAR_SATURATION,
        foreground = foreground,
    )
    return background to foreground
}

private fun mostColorfulBackground(
    hue: Float,
    saturation: Float,
    foreground: Color,
): Color {
    val foregroundIsLight = foreground.relativeLuminance() > 0.5
    var lo = if (foregroundIsLight) 0f else 0.5f
    var hi = if (foregroundIsLight) 0.5f else 1f
    var bestLightness = if (foregroundIsLight) lo else hi
    repeat(CONTRAST_SEARCH_STEPS) {
        val mid = (lo + hi) / 2f
        val meetsTarget = contrastRatio(
            Color.hsl(hue, saturation, mid),
            foreground,
        ) >= TARGET_CONTRAST_RATIO
        if (foregroundIsLight) {
            if (meetsTarget) {
                bestLightness = mid
                lo = mid
            } else {
                hi = mid
            }
        } else {
            if (meetsTarget) {
                bestLightness = mid
                hi = mid
            } else {
                lo = mid
            }
        }
    }
    return Color.hsl(hue, saturation, bestLightness)
}

internal fun contrastRatio(a: Color, b: Color): Double {
    val la = a.relativeLuminance()
    val lb = b.relativeLuminance()
    val (bright, dark) = if (la >= lb) la to lb else lb to la
    return (bright + 0.05) / (dark + 0.05)
}

private fun Color.relativeLuminance(): Double {
    fun channel(value: Double): Double =
        if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)

    return 0.2126 * channel(red.toDouble()) +
        0.7152 * channel(green.toDouble()) +
        0.0722 * channel(blue.toDouble())
}

private const val INITIAL_FONT_RATIO = 0.45f
private const val FALLBACK_ICON_RATIO = 0.6f
private const val AVATAR_SATURATION = 0.65f
private const val TARGET_CONTRAST_RATIO = 7.0
private const val CONTRAST_SEARCH_STEPS = 20
