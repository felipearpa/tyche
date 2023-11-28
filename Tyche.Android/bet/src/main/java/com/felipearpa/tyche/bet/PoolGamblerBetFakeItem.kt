package com.felipearpa.tyche.bet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.ui.shimmer

@Composable
fun PoolGamblerBetFakeItem(modifier: Modifier = Modifier) {
    PoolGamblerBetItem(
        poolGamblerBet = poolGamblerBetFakeModel(),
        viewState = PoolGamblerBetItemViewState.Visualization(partialPoolGamblerBetFakeModel()),
        modifier = modifier,
        shimmerModifier = Modifier.shimmer()
    )
}

@Preview(showBackground = true)
@Composable
fun PoolGamblerBetFakeItemPreview() {
    MaterialTheme {
        Surface {
            PoolGamblerBetFakeItem(modifier = Modifier.fillMaxWidth())
        }
    }
}