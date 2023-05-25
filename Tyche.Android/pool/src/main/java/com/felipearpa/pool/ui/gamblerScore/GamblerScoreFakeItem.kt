package com.felipearpa.pool.ui.gamblerScore

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.appUi.shimmer
import com.felipearpa.core.type.Ulid
import com.felipearpa.pool.ui.PoolGamblerScoreModel

@Composable
fun GamblerScoreFakeItem(modifier: Modifier = Modifier) {
    GamblerScoreItem(
        poolGamblerScore = PoolGamblerScoreModel(
            poolId = Ulid.randomUlid().value,
            poolLayoutId = Ulid.randomUlid().value,
            poolName = "XXXXXXXXXXXXXXXXXXXX",
            gamblerId = Ulid.randomUlid().value,
            gamblerUsername = "XXXXXXXXXXXXXXXXXXXX",
            currentPosition = 1,
            beforePosition = 1,
            score = 10
        ),
        isLoggedIn = false,
        modifier = modifier,
        shimmerModifier = Modifier.shimmer()
    )
}

@Preview(showBackground = true)
@Composable
private fun GamblerScoreFakeItemPreview() {
    GamblerScoreFakeItem(modifier = Modifier.fillMaxWidth())
}