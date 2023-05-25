package com.felipearpa.tyche.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.R
import com.felipearpa.ui.DelayedTextField

@Composable
fun Filter(
    filterText: String,
    onFilterChange: (String) -> Unit,
    onAsyncFilterChange: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        DelayedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = filterText,
            onValueChange = onFilterChange,
            onDelayedValueChange = onAsyncFilterChange
        ) {
            Text(text = stringResource(id = R.string.searcher_label))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Divider()
    }
}