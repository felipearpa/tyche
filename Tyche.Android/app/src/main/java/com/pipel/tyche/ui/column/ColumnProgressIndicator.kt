package com.pipel.tyche.ui.column

import androidx.compose.foundation.layout.Column
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pipel.tyche.ui.theme.TycheTheme

@Composable
fun ColumnProgressIndicator(modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        CircularProgressIndicator()
    }
}

@Preview(showBackground = true)
@Composable
private fun ColumnProgressIndicatorPreview() {
    TycheTheme {
        ColumnProgressIndicator()
    }
}