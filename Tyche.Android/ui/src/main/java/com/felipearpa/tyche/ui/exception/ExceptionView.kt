package com.felipearpa.tyche.ui.exception

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.ui.R

@Composable
fun ExceptionView(exception: LocalizedException) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_sentiment_sad),
            contentDescription = emptyString(),
            modifier = Modifier.size(40.dp, 40.dp),
            tint = MaterialTheme.colorScheme.error
        )

        exception.errorDescription?.let { nonNullErrorDescription ->
            Text(
                text = nonNullErrorDescription,
                style = MaterialTheme.typography.errorDescription,
                textAlign = TextAlign.Center
            )
        }

        exception.failureReason?.let { nonNullFailureReason ->
            Text(
                text = nonNullFailureReason,
                style = MaterialTheme.typography.failureReason,
                textAlign = TextAlign.Center
            )
        }

        exception.recoverySuggestion?.let { nonNullRecoverySuggestion ->
            Text(
                text = nonNullRecoverySuggestion,
                style = MaterialTheme.typography.recoverySuggestion,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun ExceptionViewPreview() {
    ExceptionView(exception = UnknownLocalizedException())
}