package com.felipearpa.tyche.ui.progress

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import com.skydoves.cloudy.Cloudy

@Composable
fun ProgressContainerView(content: @Composable () -> Unit) {
    Box {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Cloudy(modifier = Modifier.fillMaxSize()) {
                content()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 5.dp)
            ) {
                content()
            }
        }

        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.width(64.dp))
        }
    }
}