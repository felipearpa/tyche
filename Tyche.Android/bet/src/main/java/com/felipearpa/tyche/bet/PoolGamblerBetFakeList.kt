package com.felipearpa.tyche.bet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PoolGamblerBetFakeList(count: Int) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(count) {
            item {
                PoolGamblerBetFakeItem(modifier = Modifier.fillMaxWidth())
                Divider(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PoolGamblerBetFakeListPreview() {
    PoolGamblerBetFakeList(count = 10)
}