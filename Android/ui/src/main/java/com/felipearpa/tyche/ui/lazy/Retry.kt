package com.felipearpa.tyche.ui.lazy

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.ui.R

@Composable
fun Retry(modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        OutlinedButton(onClick = onRetry) {
            Text(text = stringResource(R.string.retry_action))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RetryPreview() {
    Retry {}
}