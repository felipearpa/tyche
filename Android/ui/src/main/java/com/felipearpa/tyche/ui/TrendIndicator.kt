package com.felipearpa.tyche.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.ui.theme.LocalExtendedColorScheme
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlin.math.abs

@Composable
fun TrendIndicator(
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
    difference: Int,
) {
    when {
        difference > 0 -> UpTrendIndicator(
            modifier = modifier,
            shimmerModifier = shimmerModifier,
            progress = difference,
        )

        difference < 0 -> DownTrendIndicator(
            modifier = modifier,
            shimmerModifier = shimmerModifier,
            progress = difference,
        )

        else -> StableTrendIndicator(modifier = modifier, shimmerModifier = shimmerModifier)
    }
}

@Composable
private fun StableTrendIndicator(
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Icon(
            modifier = shimmerModifier
                .size(width = ICON_SIZE.dp, ICON_SIZE.dp)
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
    shimmerModifier: Modifier = Modifier,
    progress: Int,
) {
    Row(modifier = modifier.testTag("upProgressIndicator")) {
        Icon(
            modifier = shimmerModifier.size(width = ICON_SIZE.dp, height = ICON_SIZE.dp),
            painter = painterResource(id = R.drawable.ic_arrow_upward),
            contentDescription = emptyString(),
            tint = LocalExtendedColorScheme.current.gain,
        )
        Text(
            text = abs(progress).toString(),
            style = MaterialTheme.typography.labelSmall,
            color = LocalExtendedColorScheme.current.gain,
            modifier = shimmerModifier,
        )
    }
}

@Composable
private fun DownTrendIndicator(
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
    progress: Int,
) {
    Row(modifier = modifier.testTag("downProgressIndicator")) {
        Icon(
            modifier = shimmerModifier.size(width = ICON_SIZE.dp, height = ICON_SIZE.dp),
            painter = painterResource(id = R.drawable.ic_arrow_downward),
            contentDescription = emptyString(),
            tint = LocalExtendedColorScheme.current.drop,
        )
        Text(
            text = abs(progress).toString(),
            style = MaterialTheme.typography.labelSmall,
            color = LocalExtendedColorScheme.current.drop,
            modifier = shimmerModifier,
        )
    }
}

private const val ICON_SIZE = 24

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
            UpTrendIndicator(progress = 1)
        }
    }
}

@PreviewLightDark
@Composable
private fun DownTrendIndicatorPreview() {
    TycheTheme {
        Surface {
            DownTrendIndicator(progress = 1)
        }
    }
}
