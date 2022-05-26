package com.pipel.tyche.ui.column

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
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
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_sentiment_neutral_24),
            contentDescription = String.empty(),
            modifier = Modifier.size(24.dp, 24.dp)
        )

        Text(text = stringResource(id = R.string.empty_list))
    }
}

@Preview(showBackground = true)
@Composable
private fun ColumnEmptyPreview() {
    TycheTheme {
        ColumnEmpty()
    }
}