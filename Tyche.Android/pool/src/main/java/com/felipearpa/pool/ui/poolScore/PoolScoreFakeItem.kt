package com.felipearpa.pool.ui.poolScore

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.appUi.shimmer
import com.felipearpa.core.type.Ulid
import com.felipearpa.pool.ui.PoolGamblerScoreModel

@Composable
fun PoolScoreFakeItem(modifier: Modifier = Modifier) {
    PoolScoreItem(
        poolGamblerScore = PoolGamblerScoreModel(
            poolId = Ulid.randomUlid().toString(),
            poolLayoutId = Ulid.randomUlid().toString(),
            poolName = "XXXXXXXXXXXXXXXXXXXX",
            gamblerId = Ulid.randomUlid().value,
            gamblerUsername = "XXXXXXXXXXXXXXXXXXXX",
            currentPosition = 1,
            beforePosition = 2,
            score = 10
        ),
        modifier = modifier,
        shimmerModifier = Modifier.shimmer()
    )
}

@Preview(showBackground = true)
@Composable
private fun PoolScoreFakeItemPreview() {
    PoolScoreFakeItem(modifier = Modifier.fillMaxWidth())
}