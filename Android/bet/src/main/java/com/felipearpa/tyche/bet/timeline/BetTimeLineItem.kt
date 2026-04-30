package com.felipearpa.tyche.bet.timeline

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.finished.FinishedBetItem
import com.felipearpa.tyche.bet.live.LiveBetItem

@Composable
fun BetTimeLineItem(
    bet: PoolGamblerBetModel,
    modifier: Modifier,
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
