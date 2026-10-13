package com.felipearpa.tyche.bet.kickoffgroup

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.bet.R
import com.felipearpa.tyche.ui.exception.LocalizedException

/** Recoverable failures specific to the grouped History and Timeline lists. */
sealed class KickoffGroupFeedLocalizedException : LocalizedException() {
    /**
     * The server rejected the continuation again right after an automatic restart, so the list must
     * load from the start. Its retry restarts from the first page instead of repeating the failed
     * page.
     */
    class ListChanged : KickoffGroupFeedLocalizedException() {
        override val errorDescription: String
            @Composable get() = stringResource(R.string.history_list_changed_title)

        override val failureReason: String
            @Composable get() = stringResource(R.string.history_list_changed_message)
    }
}
