package com.pipel.tyche.ui.column

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pipel.core.empty
import com.pipel.tyche.R
import com.pipel.tyche.ui.theme.TycheTheme

@Composable
fun ColumnError(modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_sentiment_very_dissatisfied_24),
                    contentDescription = String.empty(),
                    modifier = Modifier.size(48.dp, 48.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.retry_title),
                        textAlign = TextAlign.Start,
                        softWrap = true
                    )

                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(onClick = onRetry) {
                            Text(text = stringResource(R.string.retry))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ColumnErrorPreview() {
    TycheTheme {
        ColumnError {}
    }
}