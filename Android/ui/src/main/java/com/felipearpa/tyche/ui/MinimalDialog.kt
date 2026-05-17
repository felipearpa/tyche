package com.felipearpa.tyche.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing

@Composable
fun MinimalDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LocalBoxSpacing.current.medium),
            shape = RoundedCornerShape(16.dp),
        ) {
            content()
        }
    }
}
