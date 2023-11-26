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
fun FailureAlertDialog(exception: LocalizedException, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss)
            { Text(text = stringResource(id = R.string.done_action)) }
        },
        title = { Text(text = exception.errorDescription ?: emptyString()) },
        text = { Text(text = exception.failureReason ?: emptyString()) },
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
private fun FailureAlertDialogPreview() {
    FailureAlertDialog(exception = UnknownLocalizedException(), onDismiss = {})
}