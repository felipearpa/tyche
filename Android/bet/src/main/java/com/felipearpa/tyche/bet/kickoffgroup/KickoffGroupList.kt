package com.felipearpa.tyche.bet.kickoffgroup

import androidx.compose.foundation.lazy.LazyListScope
import androidx.paging.compose.LazyPagingItems
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.finished.HistoryMatchDateFormat
import com.felipearpa.tyche.bet.finished.HistoryOwner
import com.felipearpa.tyche.ui.lazy.lazyPagingConcatenateError

/**
 * The loaded groups of History or Timeline, one lazy item each, keyed by kickoff instant. The
 * feed returns a kickoff once per list (see [KickoffGroupFeed]), so a key never repeats.
 */
internal fun LazyListScope.kickoffGroupItems(
    lazyKickoffGroups: LazyPagingItems<KickoffGroupModel>,
    dateFormat: HistoryMatchDateFormat,
    owner: HistoryOwner = HistoryOwner.SignedInGambler,
    onMatchOpen: ((PoolGamblerBetModel) -> Unit)?,
) {
    items(
        count = lazyKickoffGroups.itemCount,
        key = { index -> lazyKickoffGroups.peek(index)?.key ?: index },
        contentType = { KICKOFF_GROUP_CONTENT_TYPE },
    ) { index ->
        val group = lazyKickoffGroups[index] ?: return@items
        KickoffGroupSection(group = group, dateFormat = dateFormat, owner = owner, onMatchOpen = onMatchOpen)
    }
}

/**
 * A failed append below the loaded groups. After the server rejected the continuation again, its
 * Retry restarts the list from the first page; any other failure repeats the failed page.
 */
internal fun LazyListScope.kickoffGroupAppendError(exception: Throwable, onRetry: () -> Unit, onRestart: () -> Unit) {
    lazyPagingConcatenateError(
        exception = exception,
        onRetry = if (exception is KickoffGroupFeedLocalizedException.ListChanged) onRestart else onRetry,
    )
}
