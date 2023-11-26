package com.felipearpa.tyche.pool.gamblerscore

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.pool.poolGamblerScoreFakeModel
import com.felipearpa.tyche.ui.shimmer

@Composable
fun GamblerScoreFakeItem(modifier: Modifier = Modifier) {
    GamblerScoreItem(
        poolGamblerScore = poolGamblerScoreFakeModel(),
        isLoggedIn = false,
        modifier = modifier,
        shimmerModifier = Modifier.shimmer()
    )
}

@Preview
@Composable
private fun GamblerScoreFakeItemPreview() {
    Surface {
        GamblerScoreFakeItem(modifier = Modifier.fillMaxWidth())
    }
}