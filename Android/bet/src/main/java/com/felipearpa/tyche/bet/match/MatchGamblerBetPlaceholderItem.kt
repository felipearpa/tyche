package com.felipearpa.tyche.bet.match

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.bet.poolGamblerBetFakeModel
import com.felipearpa.tyche.ui.shimmer

@Composable
fun MatchGamblerBetPlaceholderItem(modifier: Modifier = Modifier) {
    MatchGamblerBetItem(
        poolGamblerBet = poolGamblerBetFakeModel(),
        modifier = modifier,
        shimmerModifier = Modifier.shimmer(),
    )
}

@Preview(showBackground = true)
@Composable
private fun MatchGamblerBetPlaceholderItemPreview() {
    MatchGamblerBetPlaceholderItem(modifier = Modifier.fillMaxWidth())
}
