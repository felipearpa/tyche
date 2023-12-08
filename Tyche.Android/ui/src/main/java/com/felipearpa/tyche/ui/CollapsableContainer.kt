package com.felipearpa.tyche.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
fun CollapsableContainer(
    modifier: Modifier = Modifier,
    collapsableTop: @Composable BoxScope.() -> Unit,
    mainContent: @Composable BoxScope.() -> Unit
) {
    var collapsableTopHeight by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableFloatStateOf(0f) }
    var isCollapsableTopHeightGotten by remember { mutableStateOf(false) }

    fun calculateOffset(delta: Float): Offset {
        val oldOffset = offset
        val newOffset = (oldOffset + delta).coerceIn(-collapsableTopHeight, 0f)
        offset = newOffset
        return Offset(0f, newOffset - oldOffset)
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                when {
                    available.y >= 0 -> Offset.Zero
                    offset == -collapsableTopHeight -> Offset.Zero
                    else -> calculateOffset(available.y)
                }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset =
                when {
                    available.y <= 0 -> Offset.Zero
                    offset == 0f -> Offset.Zero
                    else -> calculateOffset(available.y)
                }
        }
    }

    Box(modifier = modifier.nestedScroll(nestedScrollConnection)) {
        Box(
            modifier = Modifier
                .onSizeChanged { size ->
                    collapsableTopHeight = size.height.toFloat()
                    isCollapsableTopHeightGotten = true
                }
                .offset { IntOffset(x = 0, y = offset.roundToInt()) },
            content = collapsableTop,
        )
        Box(
            modifier = Modifier
                .padding(top = with(LocalDensity.current) {
                    (collapsableTopHeight + offset).roundToInt().toDp()
                })
        ) {
            if (isCollapsableTopHeightGotten)
                mainContent()
        }
    }
}