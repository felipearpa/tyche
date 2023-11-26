package com.felipearpa.tyche.bet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.ui.shimmer
import java.time.LocalDateTime

@Composable
fun PoolGamblerBetFakeItem(modifier: Modifier = Modifier) {
    PoolGamblerBetItem(
        poolGamblerBet = PoolGamblerBetModel(
            poolId = "X".repeat(15),
            gamblerId = "X".repeat(15),
            matchId = "X".repeat(15),
            homeTeamId = "X".repeat(15),
            homeTeamName = "XXXXXXXXXXXXXXXXXXXX",
            matchScore = TeamScore(homeTeamValue = 1, awayTeamValue = 1),
            betScore = TeamScore(homeTeamValue = 1, awayTeamValue = 1),
            awayTeamId = "X".repeat(15),
            awayTeamName = "XXXXXXXXXXXXXXXXXXXX",
            score = 10,
            matchDateTime = LocalDateTime.now()
        ),
        shimmerModifier = Modifier.shimmer(),
        bet = {},
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun PoolGamblerBetFakeItemPreview() {
    MaterialTheme {
        PoolGamblerBetFakeItem(modifier = Modifier.fillMaxWidth())
    }
}