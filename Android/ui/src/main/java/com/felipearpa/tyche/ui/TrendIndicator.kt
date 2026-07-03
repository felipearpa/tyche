package com.felipearpa.tyche.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.ui.theme.LocalExtendedColorScheme
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlin.math.abs

@Composable
fun TrendIndicator(
    modifier: Modifier = Modifier,
    placeholderModifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall,
    rank: Int,
) {
    when {
        rank > 0 -> UpTrendIndicator(
            modifier = modifier,
            placeholderModifier = placeholderModifier,
            textStyle = textStyle,
            rank = rank,
        )

        rank < 0 -> DownTrendIndicator(
            modifier = modifier,
            placeholderModifier = placeholderModifier,
            textStyle = textStyle,
            rank = rank,
        )

        else -> StableTrendIndicator(
            modifier = modifier,
            placeholderModifier = placeholderModifier,
            textStyle = textStyle,
        )
    }
}

@Composable
private fun iconSizeFor(textStyle: TextStyle): Dp =
    with(LocalDensity.current) { textStyle.fontSize.toDp() * ICON_SCALE }

@Composable
private fun StableTrendIndicator(
    modifier: Modifier = Modifier,
    placeholderModifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall,
) {
    Box(modifier = modifier) {
        Icon(
            modifier = placeholderModifier
                .size(iconSizeFor(textStyle))
                .testTag("stableProgressIndicator"),
            painter = painterResource(id = R.drawable.ic_horizontal_rule),
            contentDescription = emptyString(),
            tint = LocalExtendedColorScheme.current.steady,
        )
    }
}

@Composable
private fun UpTrendIndicator(
    modifier: Modifier = Modifier,
    placeholderModifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall,
    rank: Int,
) {
    Row(
        modifier = modifier.testTag("upProgressIndicator"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = placeholderModifier.size(iconSizeFor(textStyle)),
            painter = painterResource(id = R.drawable.ic_arrow_upward),
            contentDescription = emptyString(),
            tint = LocalExtendedColorScheme.current.gain,
        )
        Text(
            text = abs(rank).toString(),
            style = textStyle,
            color = LocalExtendedColorScheme.current.gain,
            modifier = placeholderModifier,
        )
    }
}

@Composable
private fun DownTrendIndicator(
    modifier: Modifier = Modifier,
    placeholderModifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall,
    rank: Int,
) {
    Row(
        modifier = modifier.testTag("downProgressIndicator"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = placeholderModifier.size(iconSizeFor(textStyle)),
            painter = painterResource(id = R.drawable.ic_arrow_downward),
            contentDescription = emptyString(),
            tint = LocalExtendedColorScheme.current.drop,
        )
        Text(
            text = abs(rank).toString(),
            style = textStyle,
            color = LocalExtendedColorScheme.current.drop,
            modifier = placeholderModifier,
        )
    }
}

// Icon sized proportionally to the text (~line height), e.g. 16.5.dp at 11.sp labelSmall.
private const val ICON_SCALE = 1.5f

@PreviewLightDark
@Composable
private fun StableProgressIndicatorPreview() {
    TycheTheme {
        Surface {
            StableTrendIndicator()
        }
    }
}

@PreviewLightDark
@Composable
private fun UpTrendIndicatorPreview() {
    TycheTheme {
        Surface {
            UpTrendIndicator(rank = 1)
        }
    }
}

@PreviewLightDark
@Composable
private fun DownTrendIndicatorPreview() {
    TycheTheme {
        Surface {
            DownTrendIndicator(rank = 1)
        }
    }
}
