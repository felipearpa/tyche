package com.felipearpa.tyche.ui.exception

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.ui.R
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme

@Composable
fun ExceptionAlertDialog(exception: LocalizedException, onDismiss: () -> Unit) {
    val errorDescription = exception.errorDescription.orEmpty()
    val failureReason = exception.failureReason.orEmpty()
    val recoverySuggestion = exception.recoverySuggestion.orEmpty()

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
            Text(text = errorDescription)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = failureReason,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = recoverySuggestion,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_sentiment_sad),
                contentDescription = null,
                modifier = Modifier.size(width = iconSize, height = iconSize),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        },
    )
}

private val iconSize = 64.dp

@PreviewLightDark
@Composable
private fun ExceptionAlertDialogPreview() {
    TycheTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ExceptionAlertDialog(exception = UnknownLocalizedException(), onDismiss = {})
        }
    }
}
