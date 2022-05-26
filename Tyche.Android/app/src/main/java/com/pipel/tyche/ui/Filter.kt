package com.pipel.tyche.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pipel.tyche.R
import com.pipel.ui.SearcherTextField

@Composable
fun Filter(filterText: String, onFilterChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        SearcherTextField(
            modifier = Modifier.fillMaxWidth(),
            value = filterText,
            onValueChange = onFilterChange
        ) {
            Text(text = stringResource(id = R.string.searcher_label))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Divider()
    }
}