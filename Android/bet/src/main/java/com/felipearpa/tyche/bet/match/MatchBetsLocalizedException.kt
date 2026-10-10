package com.felipearpa.tyche.bet.match

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.bet.R
import com.felipearpa.tyche.ui.exception.LocalizedException

/** Recoverable failures specific to the match screen's ranked participant list. */
sealed class MatchBetsLocalizedException : LocalizedException() {
    /** The pool's ranking is not ready or could not be read. There is no other order to show. */
    class RankingUnavailable : MatchBetsLocalizedException() {
        override val errorDescription: String
            @Composable get() = stringResource(R.string.match_bets_ranking_unavailable_title)

        override val failureReason: String
            @Composable get() = stringResource(R.string.match_bets_ranking_unavailable_message)
    }

    /**
     * The server no longer accepts the list's continuation, so the list must load from the start.
     * Its retry restarts from the first page instead of repeating the failed page.
     */
    class ListChanged : MatchBetsLocalizedException() {
        override val errorDescription: String
            @Composable get() = stringResource(R.string.match_bets_list_changed_title)

        override val failureReason: String
            @Composable get() = stringResource(R.string.match_bets_list_changed_message)
    }
}
