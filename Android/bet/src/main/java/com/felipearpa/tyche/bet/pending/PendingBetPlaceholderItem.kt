package com.felipearpa.tyche.bet.pending

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.bet.poolGamblerBetFakeModel
import com.felipearpa.tyche.ui.shimmer

@Composable
fun PendingBetPlaceholderItem(modifier: Modifier = Modifier) {
    PendingBetItem(
        poolGamblerBet = poolGamblerBetFakeModel(),
        viewState = PendingBetItemViewState.Visualization(partialPoolGamblerBetFakeModel()),
        modifier = modifier,
        shimmerModifier = Modifier.shimmer()
    )
}

@Preview(showBackground = true)
@Composable
private fun PendingBetFakeItemPreview() {
    PendingBetPlaceholderItem(modifier = Modifier.fillMaxWidth())
}
