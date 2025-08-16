package com.felipearpa.tyche.bet.pending

fun PendingBetItemViewState.Companion.dummyVisualization(): PendingBetItemViewState {
    return PendingBetItemViewState.Visualization(
        PartialPoolGamblerBetModel(
            homeTeamBet = "2",
            awayTeamBet = "1"
        )
    )
}