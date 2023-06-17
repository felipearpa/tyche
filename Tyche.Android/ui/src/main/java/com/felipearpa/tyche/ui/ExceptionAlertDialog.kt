package com.felipearpa.tyche.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.core.emptyString

@Composable
fun ExceptionAlertDialog(failure: LocalizedException, onDismissClick: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissClick,
        confirmButton = {
            TextButton(onClick = onDismissClick)
            { Text(text = stringResource(id = R.string.done_action)) }
        },
        title = { Text(text = failure.failureReason ?: emptyString()) },
        text = { Text(text = failure.errorDescription ?: emptyString()) },
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_sentiment_sad),
                contentDescription = emptyString(),
                modifier = Modifier.size(40.dp, 40.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
    )
}

@Composable
@Preview
private fun ExceptionAlertDialogPreview() {
    ExceptionAlertDialog(failure = UnknownLocalizedException(), onDismissClick = {})
}