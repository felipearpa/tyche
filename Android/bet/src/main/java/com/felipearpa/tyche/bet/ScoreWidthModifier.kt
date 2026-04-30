package com.felipearpa.tyche.bet

import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.scoreWidth(): Modifier {
    if (LocalInspectionMode.current) return this.width(24.dp)

    val widthInDp = with(LocalDensity.current) { LocalTextStyle.current.fontSize.toDp() } * 3
    return this.width(widthInDp)
}
