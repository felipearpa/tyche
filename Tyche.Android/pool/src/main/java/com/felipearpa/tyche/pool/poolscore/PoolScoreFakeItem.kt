package com.felipearpa.tyche.pool.poolscore

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.pool.poolGamblerScoreFakeModel
import com.felipearpa.tyche.ui.shimmer

@Composable
fun PoolScoreFakeItem(modifier: Modifier = Modifier) {
    PoolScoreItem(
        poolGamblerScore = poolGamblerScoreFakeModel(),
        onJoinClick = {},
        modifier = modifier,
        shimmerModifier = Modifier.shimmer(),
    )
}

@Preview(showBackground = true)
@Composable
private fun PoolScoreFakeItemPreview() {
    PoolScoreFakeItem(modifier = Modifier.fillMaxWidth())
}
