package com.felipearpa.tyche.bet.finished

/**
 * Whose bets and points History's shared presentation describes. It selects the ownership
 * wording of every visible and spoken text: the signed-in gambler's History says "Your bet",
 * while another gambler's Timeline uses neutral wording and names that gambler where an
 * announcement needs an owner.
 */
sealed interface HistoryOwner {
    /** The signed-in gambler's own History. The default for every History component. */
    data object SignedInGambler : HistoryOwner

    /** Another gambler, identified by the display name shown on their Timeline. */
    data class SelectedGambler(val name: String) : HistoryOwner
}
