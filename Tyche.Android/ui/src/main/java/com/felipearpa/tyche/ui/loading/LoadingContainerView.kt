package com.felipearpa.tyche.ui.loading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.ui.preview.UIModePreview
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.LocalExtendedColorScheme
import com.felipearpa.tyche.ui.theme.TycheTheme

private const val BLUR_RADIUS = 2

@Composable
fun LoadingContainerView(content: @Composable () -> Unit) {
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
                .background(LocalExtendedColorScheme.current.loadingBackground)
        ) {
            BallSpinner()
        }
    }
}

@UIModePreview
@Composable
private fun ProgressContainerViewPreview() {
    TycheTheme {
        Surface {
            LoadingContainerView {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = LocalBoxSpacing.current.medium),
                        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium)
                    ) {
                        TextField(
                            value = "",
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = { /*TODO*/ }, modifier = Modifier.fillMaxWidth()) {
                            Text(text = "Ok")
                        }
                    }
                }
            }
        }
    }
}