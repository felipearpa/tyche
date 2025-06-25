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

private const val ICON_SIZE = 24

@Composable
private fun StableProgressIndicator(
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
private fun UpProgressIndicator(
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
private fun DownProgressIndicator(
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

@Composable
fun ProgressIndicator(
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
    difference: Int,
) {
    when {
        difference > 0 -> UpProgressIndicator(
            modifier = modifier,
            shimmerModifier = shimmerModifier,
            progress = difference,
        )

        difference < 0 -> DownProgressIndicator(
            modifier = modifier,
            shimmerModifier = shimmerModifier,
            progress = difference,
        )

        else -> StableProgressIndicator(modifier = modifier, shimmerModifier = shimmerModifier)
    }
}

@PreviewLightDark
@Composable
private fun StableProgressIndicatorPreview() {
    TycheTheme {
        Surface {
            StableProgressIndicator()
        }
    }
}

@PreviewLightDark
@Composable
private fun UpProgressIndicatorPreview() {
    TycheTheme {
        Surface {
            UpProgressIndicator(progress = 1)
        }
    }
}

@PreviewLightDark
@Composable
private fun DownProgressIndicatorPreview() {
    TycheTheme {
        Surface {
            DownProgressIndicator(progress = 1)
        }
    }
}
