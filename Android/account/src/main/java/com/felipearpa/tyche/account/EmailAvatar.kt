package com.felipearpa.tyche.account

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.ui.theme.TycheTheme
import org.koin.compose.koinInject
import kotlin.math.pow
import com.felipearpa.tyche.ui.R as SharedR

@Composable
fun AutoEmailAvatar(modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) {
        EmailAvatar(
            email = "felipearpa@tyche.com",
            modifier = modifier,
        )
        return
    }

    val accountStorage = koinInject<AccountStorage>()
    val account by accountStorage.state.collectAsStateWithLifecycle()
    EmailAvatar(
        email = account?.email.orEmpty(),
        modifier = modifier,
    )
}

@Composable
fun EmailAvatar(email: String, modifier: Modifier = Modifier) {
    val initial = email.avatarInitial()
    if (initial == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            BoxWithConstraints {
                val iconSize = minOf(maxWidth, maxHeight) * FALLBACK_ICON_RATIO
                Icon(
                    painter = painterResource(id = SharedR.drawable.filled_person),
                    contentDescription = emptyString(),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
    } else {
        val (background, foreground) = avatarColorsFor(email)
        BoxWithConstraints(
            modifier = modifier.background(background),
            contentAlignment = Alignment.Center,
        ) {
            val diameter = minOf(maxWidth, maxHeight)
            val fontSize = with(LocalDensity.current) {
                (diameter * INITIAL_FONT_RATIO).toSp()
            }
            Text(
                text = initial.toString(),
                color = foreground,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

fun Modifier.navigationEmailAvatar() =
    size(NAVIGATION_AVATAR_SIZE.dp)
        .clip(CircleShape)

private fun String.avatarInitial(): Char? =
    substringBefore('@').firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()

@Composable
private fun avatarColorsFor(key: String): Pair<Color, Color> =
    avatarColorsFor(key = key, isDark = isSystemInDarkTheme())

private fun avatarColorsFor(key: String, isDark: Boolean): Pair<Color, Color> {
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

private fun contrastRatio(a: Color, b: Color): Double {
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

private const val NAVIGATION_AVATAR_SIZE = 32
private const val INITIAL_FONT_RATIO = 0.45f
private const val FALLBACK_ICON_RATIO = 0.6f
private const val AVATAR_SATURATION = 0.65f
private const val TARGET_CONTRAST_RATIO = 7.0
private const val CONTRAST_SEARCH_STEPS = 20

@PreviewLightDark
@Composable
private fun EmailAvatarPreview() {
    TycheTheme {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            EmailAvatar(
                email = "felipearpa@email.com",
                modifier = Modifier.size(64.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun EmailAvatarFallbackPreview() {
    TycheTheme {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            EmailAvatar(
                email = "@email.com",
                modifier = Modifier.size(64.dp),
            )
        }
    }
}
