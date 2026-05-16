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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.ui.theme.TycheTheme

@Composable
fun PositionIndicator(
    position: Int?,
    shouldUsePrimaryColor: Boolean,
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(scoreSize)
            .clip(RoundedCornerShape(8.dp))
            .background(color = if (shouldUsePrimaryColor) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
            .then(shimmerModifier),
        contentAlignment = Alignment.Center,
    ) {
        position?.let {
            Text(
                text = it.toString(),
                color = if (shouldUsePrimaryColor) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
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
private fun SignedInPositionIndicatorWithoutPositionPreview() {
    TycheTheme {
        Surface {
            PositionIndicator(
                position = null,
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
                position = 1,
                shouldUsePrimaryColor = false,
            )
        }
    }
}
