package com.felipearpa.tyche.bet.pending

import com.felipearpa.tyche.core.type.TeamScore

/** The prediction the server holds for a match: none yet, or the placed scores. */
sealed interface SavedBet {
    data object None : SavedBet

    data class Placed(val score: TeamScore<Int>) : SavedBet

    /** The scores at the model and request edge, where an absent prediction is a null score. */
    fun scoreOrNull(): TeamScore<Int>? = when (this) {
        None -> null
        is Placed -> score
    }

    companion object {
        /** The feed's nullable prediction as a saved bet. */
        fun of(score: TeamScore<Int>?): SavedBet = if (score == null) None else Placed(score)
    }
}
