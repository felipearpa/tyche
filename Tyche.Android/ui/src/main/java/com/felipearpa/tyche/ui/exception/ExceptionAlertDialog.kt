package com.felipearpa.tyche.ui.exception

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.ui.R

@Composable
fun ExceptionAlertDialog(exception: LocalizedException, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.done_action))
            }
        },
        title = {
            Text(text = exception.errorDescription.orEmpty())
        },
        text = {
            Text(
                text = listOfNotNull(
                    exception.failureReason,
                    exception.recoverySuggestion,
                ).joinToString(separator = ". "),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_sentiment_sad),
                contentDescription = null,
                modifier = Modifier.size(width = iconSize, height = iconSize),
            )
        },
    )
}

private val iconSize = 64.dp

@Composable
@Preview
private fun ExceptionAlertDialogPreview() {
    ExceptionAlertDialog(exception = UnknownLocalizedException(), onDismiss = {})
}
