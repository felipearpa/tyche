package com.felipearpa.tyche.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SwipeToRevealBox(
    isRevealed: Boolean,
    modifier: Modifier = Modifier,
    onExpanded: () -> Unit = {},
    onCollapsed: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    var actionsBoxWidth by remember { mutableFloatStateOf(0f) }
    val offset = remember { Animatable(initialValue = 0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = isRevealed, key2 = actionsBoxWidth) {
        if (isRevealed) {
            offset.animateTo(-actionsBoxWidth)
        } else {
            offset.animateTo(0f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.onSizeChanged { actionsBoxWidth = it.width.toFloat() },
            ) {
                actions()
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(x = offset.value.roundToInt(), y = 0) }
                .pointerInput(actionsBoxWidth) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            println("FELIPE -> $dragAmount")
                            scope.launch {
                                val newOffset = (offset.value + dragAmount)
                                    .coerceIn(
                                        minimumValue = -actionsBoxWidth,
                                        maximumValue = 0f,
                                    )
                                offset.snapTo(newOffset)
                            }
                        },
                        onDragEnd = {
                            when {
                                abs(offset.value) >= actionsBoxWidth / 2f -> {
                                    scope.launch {
                                        offset.animateTo(-actionsBoxWidth)
                                        onExpanded()
                                    }
                                }

                                else -> {
                                    scope.launch {
                                        offset.animateTo(0f)
                                        onCollapsed()
                                    }
                                }
                            }
                        },
                    )
                },
        ) {
            content()
        }
    }
}
