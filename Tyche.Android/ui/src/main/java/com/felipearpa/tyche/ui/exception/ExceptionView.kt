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
import com.felipearpa.tyche.ui.theme.boxSpacing

@Composable
fun ExceptionView(localizedException: LocalizedException) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.boxSpacing.large),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_sentiment_sad),
            contentDescription = emptyString(),
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.boxSpacing.medium),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = localizedException.errorDescription ?: emptyString(),
                style = MaterialTheme.typography.errorDescription,
                textAlign = TextAlign.Center
            )

            Text(
                text = listOfNotNull(
                    localizedException.failureReason,
                    localizedException.recoverySuggestion
                ).joinToString(separator = "."),
                style = MaterialTheme.typography.failureReason
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun ExceptionViewPreview() {
    ExceptionView(localizedException = UnknownLocalizedException())
}