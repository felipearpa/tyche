package com.felipearpa.tyche.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.core.emptyString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    filterValue: String,
    onFilterValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isActive by remember { mutableStateOf(false) }
    val historyItems = remember { mutableStateListOf<String>() }

    SearchBar(
        modifier = modifier,
        query = filterValue,
        onQueryChange = onFilterValueChange,
        onSearch = { newSearch ->
            isActive = false
            historyItems.add(newSearch)
        },
        active = isActive,
        onActiveChange = { newActive ->
            isActive = newActive
        },
        placeholder = {
            Text(text = stringResource(id = R.string.searching_label))
        },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.search),
                contentDescription = emptyString()
            )
        }
    ) {
        historyItems.forEach { historyItem ->
            Row(modifier = Modifier.padding(all = 16.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.history),
                    contentDescription = emptyString(),
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(text = historyItem)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchBarPreview() {
    SearchBar(filterValue = emptyString(), onFilterValueChange = {})
}