package com.felipearpa.tyche.ui.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.ui.R
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.exception.localizedOrDefault
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing

fun LazyListScope.lazyPagingConcatenateError(exception: Throwable, retry: () -> Unit) {
    item {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LocalBoxSpacing.current.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
        ) {
            Failure(
                localizedException = exception.localizedOrDefault(),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(onClick = { retry() }) {
                Text(text = stringResource(id = R.string.retry_action))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LazyPagingConcatenateErrorPreview() {
    LazyColumn {
        lazyPagingConcatenateError(exception = UnknownLocalizedException(), retry = {})
    }
}
