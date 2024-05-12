package com.felipearpa.tyche.ui.loading

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.ui.R
import com.felipearpa.tyche.ui.preview.UIModePreview
import com.felipearpa.tyche.ui.theme.TycheTheme

private const val labelAnimation = "ProgressIndicator"
private const val iconSize = 64

@Composable
fun BallSpinner(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = labelAnimation)
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = labelAnimation
    )

    Icon(
        painter = painterResource(id = R.drawable.tyche_logo),
        contentDescription = emptyString(),
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .size(width = iconSize.dp, height = iconSize.dp)
            .rotate(angle)
    )
}

@UIModePreview
@Composable
private fun ProgressIndicatorPreview() {
    TycheTheme {
        Surface {
            BallSpinner()
        }
    }
}