package com.felipearpa.tyche.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing

private const val ICON_SIZE = 40

@Composable
fun IconMessage(@DrawableRes iconResourceId: Int, @StringRes messageResourceId: Int) {
    IconMessage(iconResourceId = iconResourceId, message = stringResource(id = messageResourceId))
}

@Composable
fun IconMessage(@DrawableRes iconResourceId: Int, message: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(id = iconResourceId),
            contentDescription = emptyString(),
            modifier = Modifier.size(width = ICON_SIZE.dp, height = ICON_SIZE.dp),
        )

        Text(text = message)
    }
}

@Preview(showBackground = true)
@Composable
private fun IconMessagePreview() {
    IconMessage(
        iconResourceId = R.drawable.ic_sentiment_sad,
        messageResourceId = R.string.unknown_failure_reason,
    )
}
