package com.felipearpa.appUi.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.appUi.R
import com.felipearpa.core.emptyString

@Composable
fun Empty(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_sentiment_sad),
            contentDescription = emptyString(),
            modifier = Modifier.size(40.dp, 40.dp)
        )

        Text(text = stringResource(id = R.string.empty_list_message))
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyPreview() {
    Empty(modifier = Modifier.fillMaxWidth())
}