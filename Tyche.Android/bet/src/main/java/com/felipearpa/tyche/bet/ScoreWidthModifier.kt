package com.felipearpa.tyche.bet

import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalDensity

fun Modifier.scoreWidth() = composed {
    val widthInDp = with(LocalDensity.current) { LocalTextStyle.current.fontSize.toDp() } * 3
    this.width(widthInDp)
}