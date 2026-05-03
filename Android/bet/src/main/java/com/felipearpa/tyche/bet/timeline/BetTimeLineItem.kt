package com.felipearpa.tyche.bet.timeline

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.finished.FinishedBetItem
import com.felipearpa.tyche.bet.live.LiveBetItem
import com.felipearpa.tyche.bet.poolGamblerBetDummyModel

@Composable
fun BetTimeLineItem(
    bet: PoolGamblerBetModel,
    modifier: Modifier = Modifier,
) {
    if (bet.isComputed) {
        FinishedBetItem(
            poolGamblerBet = bet,
            modifier = modifier,
        )
    } else {
        LiveBetItem(
            poolGamblerBet = bet,
            modifier = modifier,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BetTimelineItemPreview() {
    BetTimeLineItem(
        bet = poolGamblerBetDummyModel(),
        modifier = Modifier.fillMaxWidth(),
    )
}
