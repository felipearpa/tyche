package com.felipearpa.tyche.ui.exception

import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.ui.R

@Composable
fun ExceptionAlertDialog(exception: LocalizedException, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss)
            { Text(text = stringResource(id = R.string.done_action)) }
        },
        title = {
            exception.errorDescription?.let { errorDescription ->
                Text(text = errorDescription)
            }
        },
        text = {
            Text(
                text = listOfNotNull(
                    exception.failureReason,
                    exception.recoverySuggestion
                ).joinToString(separator = ". "),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
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
    ExceptionAlertDialog(exception = UnknownLocalizedException(), onDismiss = {})
}