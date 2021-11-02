package com.pipel.tyche.ui.column

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pipel.core.empty
import com.pipel.tyche.R
import com.pipel.tyche.ui.theme.TycheTheme

@Composable
fun ColumnEmpty(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(top = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_sentiment_neutral_24),
                contentDescription = String.empty(),
                modifier = Modifier.size(24.dp, 24.dp)
            )

            Text(text = stringResource(id = R.string.empty_list))
        }

        Divider()
    }
}

@Preview(showBackground = true)
@Composable
private fun ColumnEmptyPreview() {
    TycheTheme {
        ColumnEmpty()
    }
}