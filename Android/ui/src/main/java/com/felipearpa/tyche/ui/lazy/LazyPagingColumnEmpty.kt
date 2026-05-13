package com.felipearpa.tyche.ui.lazy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

fun LazyListScope.lazyPagingColumnEmpty() {
    item {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillParentMaxSize(),
        ) {
            Empty(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LazyPagingColumnEmpty() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        lazyPagingColumnEmpty()
    }
}
