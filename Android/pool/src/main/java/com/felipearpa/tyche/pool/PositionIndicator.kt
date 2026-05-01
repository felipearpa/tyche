package com.felipearpa.tyche.pool

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PositionIndicator(
    position: Int,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(scoreSize)
            .clip(CircleShape)
            .background(color = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
            .then(shimmerModifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = position.toString(),
            color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
fun NonPositionIndicator() {
    Spacer(modifier = Modifier.size(scoreSize))
}

private val scoreSize = 32.dp

@Preview(name = "Signed In", showBackground = true)
@Composable
private fun SignedInPositionIndicatorPreview() {
    PositionIndicator(
        position = 1,
        isCurrentUser = true,
    )
}

@Preview(name = "Not Signed In", showBackground = true)
@Composable
private fun NonSignedInPositionIndicatorPreview() {
    PositionIndicator(
        position = 1,
        isCurrentUser = false,
    )
}
