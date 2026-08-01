package com.felipearpa.tyche.pool

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.ui.theme.TycheTheme

@Composable
fun PositionIndicator(
    position: Int?,
    shouldUsePrimaryColor: Boolean,
    modifier: Modifier = Modifier,
    placeholderModifier: Modifier = Modifier,
    size: Dp = scoreSize,
    shape: Shape = RoundedCornerShape(8.dp),
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val resolvedContainerColor = if (containerColor != Color.Unspecified) {
        containerColor
    } else if (shouldUsePrimaryColor) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val resolvedContentColor = if (contentColor != Color.Unspecified) {
        contentColor
    } else if (shouldUsePrimaryColor) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(resolvedContainerColor)
            .then(placeholderModifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = position?.toString() ?: "—",
            color = resolvedContentColor,
            style = textStyle,
        )
    }
}

private val scoreSize = 32.dp

@PreviewLightDark
@Composable
private fun SignedInPositionIndicatorPreview() {
    TycheTheme {
        Surface {
            PositionIndicator(
                position = 1,
                shouldUsePrimaryColor = true,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun NonSignedInPositionIndicatorPreview() {
    TycheTheme {
        Surface {
            PositionIndicator(
                position = 1,
                shouldUsePrimaryColor = false,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun NonSignedInPositionIndicatorWithoutPositionPreview() {
    TycheTheme {
        Surface {
            PositionIndicator(
                position = null,
                shouldUsePrimaryColor = false,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun LeaderboardPositionIndicatorPreview() {
    TycheTheme {
        Surface {
            PositionIndicator(
                position = 3,
                shouldUsePrimaryColor = false,
                size = 44.dp,
                shape = RoundedCornerShape(10.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                textStyle = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
