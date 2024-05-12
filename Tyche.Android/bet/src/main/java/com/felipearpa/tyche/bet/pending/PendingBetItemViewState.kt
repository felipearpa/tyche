package com.felipearpa.tyche.bet.pending

sealed class PendingBetItemViewState(open val value: PartialPoolGamblerBetModel) {
    data class Visualization(override val value: PartialPoolGamblerBetModel) :
        PendingBetItemViewState(value)

    data class Edition(override val value: PartialPoolGamblerBetModel) :
        PendingBetItemViewState(value)

    companion object
}

fun PendingBetItemViewState.Companion.emptyVisualization(): PendingBetItemViewState {
    return PendingBetItemViewState.Visualization(emptyPartialPoolGamblerBetModel())
}

fun PendingBetItemViewState.copy(value: PartialPoolGamblerBetModel): PendingBetItemViewState {
    return when (this) {
        is PendingBetItemViewState.Visualization -> PendingBetItemViewState.Visualization(
            value
        )

        is PendingBetItemViewState.Edition -> PendingBetItemViewState.Edition(value)
    }
}