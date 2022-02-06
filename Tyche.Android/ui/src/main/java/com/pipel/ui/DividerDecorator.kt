package com.pipel.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
fun DividerDecorator(top: Dp? = null, bottom: Dp? = null, content: @Composable () -> Unit) {
    top?.let {
        Divider()
        Spacer(modifier = Modifier.height(it))
    }

    content()

    bottom?.let {
        Spacer(modifier = Modifier.height(it))
        Divider()
    }
}