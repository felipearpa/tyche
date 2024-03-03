package com.felipearpa.tyche.ui.progress

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.ui.theme.boxSpacing
import com.felipearpa.tyche.ui.theme.progressBackground

private const val BLUR_RADIUS = 2

@Composable
fun ProgressContainerView(content: @Composable () -> Unit) {
    val backgroundAlpha = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) 0.5f else 0f

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = BLUR_RADIUS.dp)
        ) {
            content()
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.progressBackground.copy(alpha = backgroundAlpha))
        ) {
            BallSpinner()
        }

    }
}

@Preview(showBackground = true)
@Composable
private fun ProgressContainerViewPreview() {
    ProgressContainerView {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = MaterialTheme.boxSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.boxSpacing.medium)
            ) {
                TextField(value = "", onValueChange = {}, modifier = Modifier.fillMaxWidth())
                Button(onClick = { /*TODO*/ }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Ok")
                }
            }
        }
    }
}