package com.felipearpa.tyche.bet

import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.scoreWidth(): Modifier {
    if (LocalInspectionMode.current) return this.width(64.dp)

    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val textMeasurer = rememberTextMeasurer()
    val textStyle = LocalTextStyle.current
    val contentPadding = OutlinedTextFieldDefaults.contentPadding()

    val widthInDp = remember(density, layoutDirection, textStyle) {
        with(density) {
            val textWidth = textMeasurer.measure("000", style = textStyle).size.width.toDp()
            val horizontalPadding =
                contentPadding.calculateStartPadding(layoutDirection) +
                        contentPadding.calculateEndPadding(layoutDirection)
            textWidth + horizontalPadding
        }
    }
    return this.width(widthInDp)
}
