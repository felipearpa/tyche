package com.felipearpa.tyche.ui.lazy

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.ui.exception.ExceptionView
import com.felipearpa.tyche.ui.exception.LocalizedException
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException

@Composable
fun Failure(localizedException: LocalizedException, modifier: Modifier = Modifier) {
    ExceptionView(localizedException = localizedException)
}

@Preview(showBackground = true)
@Composable
private fun FailurePreview() {
    Failure(localizedException = UnknownLocalizedException(), modifier = Modifier.fillMaxWidth())
}
