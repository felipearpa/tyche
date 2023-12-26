package com.felipearpa.tyche.ui.progress

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.ui.R

private const val LABEL_ANIMATION = "ProgressIndicator"

@Composable
fun ProgressIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = LABEL_ANIMATION)
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = LABEL_ANIMATION
    )

    Icon(
        painter = painterResource(id = R.drawable.tyche_logo),
        contentDescription = emptyString(),
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .size(width = 64.dp, height = 64.dp)
            .rotate(angle)
    )
}

@Preview
@Composable
private fun ProgressIndicatorPreview() {
    ProgressIndicator()
}