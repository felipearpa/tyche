package com.felipearpa.tyche.bet

import com.felipearpa.tyche.core.emptyString

sealed class PoolGamblerBetItemViewState(open val value: PartialPoolGamblerBetModel) {
    data class Visualization(override val value: PartialPoolGamblerBetModel) :
        PoolGamblerBetItemViewState(value)

    data class Edition(override val value: PartialPoolGamblerBetModel) :
        PoolGamblerBetItemViewState(value)

    companion object
}

fun PoolGamblerBetItemViewState.Companion.emptyVisualization(): PoolGamblerBetItemViewState {
    return PoolGamblerBetItemViewState.Visualization(
        PartialPoolGamblerBetModel(homeTeamBet = emptyString(), awayTeamBet = emptyString())
    )
}

fun PoolGamblerBetItemViewState.copy(value: PartialPoolGamblerBetModel): PoolGamblerBetItemViewState {
    return when (this) {
        is PoolGamblerBetItemViewState.Visualization -> PoolGamblerBetItemViewState.Visualization(
            value
        )

        is PoolGamblerBetItemViewState.Edition -> PoolGamblerBetItemViewState.Edition(value)
    }
}