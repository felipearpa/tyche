package com.felipearpa.appUi

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.felipearpa.core.emptyString

@Composable
fun Message(@DrawableRes iconResourceId: Int, @StringRes messageResourceId: Int) {
    Message(iconResourceId = iconResourceId, message = stringResource(id = messageResourceId))
}

@Composable
fun Message(@DrawableRes iconResourceId: Int, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = iconResourceId),
            contentDescription = emptyString(),
            modifier = Modifier.size(40.dp, 40.dp)
        )

        Text(text = message)
    }
}

@Preview(showBackground = true)
@Composable
fun MessagePreview() {
    Message(
        iconResourceId = R.drawable.ic_sentiment_sad,
        messageResourceId = R.string.unknown_failure_message
    )
}