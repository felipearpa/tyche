package com.felipearpa.tyche.ui.lazy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.exception.localizedOrDefault
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing

fun LazyListScope.lazyPagingColumnError(exception: Throwable) {
    item {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillParentMaxSize(),
        ) {
            Failure(
                localizedException = exception.localizedOrDefault(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = LocalBoxSpacing.current.medium),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LazyPagingColumnErrorPreview() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        lazyPagingColumnError(exception = UnknownLocalizedException())
    }
}
