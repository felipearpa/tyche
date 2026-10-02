package com.felipearpa.tyche.bet.timeline

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.finished.FinishedBetItem
import com.felipearpa.tyche.bet.live.LiveBetItem
import com.felipearpa.tyche.bet.poolGamblerBetDummyModel

/** A timeline bet; [isPlaceholder] passes through to the leaf item, which owns the pulse. */
@Composable
fun BetTimeLineItem(
    bet: PoolGamblerBetModel,
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
) {
    if (bet.isComputed) {
        FinishedBetItem(
            poolGamblerBet = bet,
            modifier = modifier,
            isPlaceholder = isPlaceholder,
        )
    } else {
        LiveBetItem(
            poolGamblerBet = bet,
            modifier = modifier,
            isPlaceholder = isPlaceholder,
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
