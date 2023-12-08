package com.felipearpa.tyche.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.core.emptyString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    filterValue: String,
    onFilterValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SearchBar(
        modifier = modifier,
        query = filterValue,
        onQueryChange = onFilterValueChange,
        onSearch = {},
        active = false,
        onActiveChange = { },
        placeholder = {
            Text(text = stringResource(id = R.string.searching_label))
        },
        trailingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.search),
                contentDescription = emptyString()
            )
        }
    ) {}
}

@Preview(showBackground = true)
@Composable
private fun SearchBarPreview() {
    SearchBar(filterValue = emptyString(), onFilterValueChange = {})
}