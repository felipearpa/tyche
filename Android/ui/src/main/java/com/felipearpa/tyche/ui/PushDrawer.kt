package com.felipearpa.tyche.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.ui.theme.TycheTheme

private const val DRAWER_WIDTH_RATIO = 0.85f
private const val ANIMATION_DURATION_MS = 300
private val CORNER_RADIUS: Dp = 25.dp
private const val OPEN_DIM_ALPHA = 0.15f
private const val DRAG_THRESHOLD_PX = 50f

@Composable
fun PushDrawer(
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dimColor = if (isSystemInDarkTheme()) Color.White else Color.Black

    BoxWithConstraints(modifier.fillMaxSize()) {
        val drawerWidth = maxWidth * DRAWER_WIDTH_RATIO

        val offsetX by animateDpAsState(
            targetValue = if (isOpen) drawerWidth else 0.dp,
            animationSpec = tween(ANIMATION_DURATION_MS, easing = FastOutSlowInEasing),
            label = "drawerOffset",
        )
        val corner by animateDpAsState(
            targetValue = if (isOpen) CORNER_RADIUS else 0.dp,
            animationSpec = tween(ANIMATION_DURATION_MS, easing = FastOutSlowInEasing),
            label = "drawerCorner",
        )
        val dim by animateFloatAsState(
            targetValue = if (isOpen) OPEN_DIM_ALPHA else 0f,
            animationSpec = tween(ANIMATION_DURATION_MS, easing = FastOutSlowInEasing),
            label = "drawerDim",
        )
        val borderAlpha by animateFloatAsState(
            targetValue = if (isOpen) 1f else 0f,
            animationSpec = tween(ANIMATION_DURATION_MS, easing = FastOutSlowInEasing),
            label = "drawerBorderAlpha",
        )

        Box(
            Modifier
                .width(drawerWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            drawerContent()
        }

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = offsetX.toPx()
                    shape = RoundedCornerShape(topStart = corner, bottomStart = corner)
                    clip = true
                }
                .then(
                    if (borderAlpha > 0f) {
                        Modifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderAlpha),
                            shape = RoundedCornerShape(topStart = corner, bottomStart = corner),
                        )
                    } else Modifier,
                )
                .pointerInput(isOpen) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragEnd = {
                            if (isOpen && totalDrag < -DRAG_THRESHOLD_PX) {
                                onOpenChange(false)
                            } else if (!isOpen && totalDrag > DRAG_THRESHOLD_PX) {
                                onOpenChange(true)
                            }
                        },
                        onDragCancel = { totalDrag = 0f },
                        onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                    )
                },
        ) {
            content()

            if (dim > 0f) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(dimColor.copy(alpha = dim))
                        .then(
                            if (isOpen) {
                                Modifier.pointerInput(Unit) {
                                    detectTapGestures { onOpenChange(false) }
                                }
                            } else Modifier,
                        ),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PushDrawerClosedPreview() {
    TycheTheme {
        Surface {
            PushDrawer(
                isOpen = false,
                onOpenChange = {},
                drawerContent = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Gray),
                    ) {
                        Text(text = "Drawer Content", modifier = Modifier.padding(16.dp))
                    }
                },
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    Text(text = "Main Content", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PushDrawerOpenedPreview() {
    TycheTheme {
        Surface {
            PushDrawer(
                isOpen = true,
                onOpenChange = {},
                drawerContent = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Gray),
                    ) {
                        Text(text = "Drawer Content", modifier = Modifier.padding(16.dp))
                    }
                },
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    Text(text = "Main Content", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}
