package com.felipearpa.tyche.ui.loading

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.felipearpa.tyche.ui.R
import com.felipearpa.tyche.ui.theme.TycheTheme

/**
 * A form screen inside the loading host: an Email field, a Submit button that counts, and a long
 * list. The screen's own remembered state counts how often it is created and disposed, so a test
 * can tell a kept screen from a rebuilt one.
 */
internal class LoadingHostHarness {
    var isLoading by mutableStateOf(false)
    var submissions = 0
        private set
    var screenCreations = 0
        private set
    var screenDisposals = 0
        private set

    /** Whether focus is anywhere in the screen, as the screen itself observes it. */
    var screenHasFocus = false
        private set
    val listState = LazyListState()

    @Composable
    fun Content() {
        TycheTheme {
            Surface {
                LoadingContainerView(isLoading = isLoading) {
                    Screen()
                }
            }
        }
    }

    @Composable
    private fun Screen() {
        var email by remember { screenCreations++; mutableStateOf("") }
        DisposableEffect(Unit) { onDispose { screenDisposals++ } }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged { screenHasFocus = it.hasFocus }
                .focusGroup()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(EmailLabel) },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { submissions++ }, modifier = Modifier.fillMaxWidth()) {
                Text(SubmitLabel)
            }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(count = RowCount) { index ->
                    Text(text = "Row $index", modifier = Modifier.padding(vertical = 12.dp))
                }
            }
        }
    }
}

internal const val EmailLabel = "Email"
internal const val SubmitLabel = "Submit"
private const val RowCount = 60

/** The localized loading status, as the device's locale renders it. */
internal val loadingStatus: String
    get() = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.loading_status)
