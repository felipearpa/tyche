package com.felipearpa.tyche.ui.lazy

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ProgressIndicator(modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        CircularProgressIndicator()
    }
}

@Preview(showBackground = true)
@Composable
private fun ProgressIndicatorPreview() {
    ProgressIndicator()
}