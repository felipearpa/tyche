package com.felipearpa.tyche.bet.pending

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.R
import com.felipearpa.tyche.bet.pending.PendingBetRowHolder.RowState
import com.felipearpa.tyche.core.type.TeamScore

/** What TalkBack hears when a card's state changes. */
internal sealed interface PendingBetAnnouncement {
    data class Saved(val score: TeamScore<Int>) : PendingBetAnnouncement
    data object Failed : PendingBetAnnouncement

    /** The server refused a save because betting has closed. */
    data object Locked : PendingBetAnnouncement

    companion object {
        /**
         * The announcement for a change from [previous] to [state]: the saved scores when a save
         * completes, the failure when it fails, the lock when a save is refused because betting has
         * closed, and nothing otherwise. A lock that arrives with a feed refresh is not announced.
         */
        fun of(previous: RowState?, state: RowState, saved: SavedBet): PendingBetAnnouncement? = when {
            state == RowState.SAVED && previous == RowState.SAVING -> (saved as? SavedBet.Placed)?.let { Saved(it.score) }
            state == RowState.FAILED && previous != RowState.FAILED -> Failed
            state == RowState.LOCKED && previous == RowState.SAVING -> Locked
            else -> null
        }
    }
}

@Composable
internal fun PendingBetAnnouncement.text(match: PoolGamblerBetModel): String = when (this) {
    is PendingBetAnnouncement.Saved -> stringResource(
        R.string.bet_saved_announcement,
        match.homeTeamName,
        score.homeTeamValue,
        match.awayTeamName,
        score.awayTeamValue,
    )

    PendingBetAnnouncement.Failed -> stringResource(R.string.bet_save_failure_announcement, match.homeTeamName, match.awayTeamName)
    PendingBetAnnouncement.Locked -> stringResource(R.string.bet_locked_announcement, match.homeTeamName, match.awayTeamName)
}
