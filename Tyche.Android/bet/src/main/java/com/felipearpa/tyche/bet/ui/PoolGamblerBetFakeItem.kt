package com.felipearpa.tyche.bet.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.core.type.Ulid
import com.felipearpa.tyche.ui.shimmer
import java.time.LocalDateTime

@Composable
fun PoolGamblerBetFakeItem(modifier: Modifier = Modifier) {
    PoolGamblerBetItem(
        poolGamblerBet = PoolGamblerBetModel(
            poolId = Ulid.randomUlid().value,
            gamblerId = Ulid.randomUlid().value,
            matchId = Ulid.randomUlid().value,
            homeTeamId = Ulid.randomUlid().value,
            homeTeamName = "XXXXXXXXXXXXXXXXXXXX",
            matchScore = TeamScore(homeTeamValue = 1, awayTeamValue = 1),
            betScore = TeamScore(homeTeamValue = 1, awayTeamValue = 1),
            awayTeamId = Ulid.randomUlid().value,
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