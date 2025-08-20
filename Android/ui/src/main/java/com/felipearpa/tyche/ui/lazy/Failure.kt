package com.felipearpa.tyche.ui.lazy

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.ui.R
import com.felipearpa.tyche.ui.exception.errorDescription

@Composable
fun Failure(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_sentiment_sad),
            contentDescription = emptyString(),
            modifier = Modifier.size(40.dp, 40.dp),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.remote_communication_failure_description),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.errorDescription,
            )

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.remote_communication_failure_reason),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.remote_communication_failure_recovery_suggestion),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FailurePreview() {
    Failure(modifier = Modifier.fillMaxWidth())
}
