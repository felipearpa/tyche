package com.pipel.tyche.ui.column

import androidx.compose.foundation.layout.Column
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pipel.tyche.R
import com.pipel.tyche.ui.theme.TycheTheme

@Composable
fun ColumnRetry(modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        OutlinedButton(onClick = onRetry) {
            Text(text = stringResource(R.string.retry))
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun ColumnRetryPreview() {
    TycheTheme {
        ColumnRetry {}
    }
}