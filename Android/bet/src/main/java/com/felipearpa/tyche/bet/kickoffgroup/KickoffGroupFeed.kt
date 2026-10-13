package com.felipearpa.tyche.bet.kickoffgroup

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.data.bet.domain.KickoffGroupFeedException
import com.felipearpa.tyche.ui.exception.orDefaultLocalized
import kotlin.time.Instant

typealias KickoffGroupQuery = suspend (next: String?) -> Result<CursorPage<KickoffGroupModel>>

/**
 * The paging rules shared by History and Timeline. The server sends whole kickoff groups in their
 * final order, so pages are appended as they arrive: nothing is merged, sorted, recounted, or
 * derived from the loaded rows. Each [pagingSource] is one generation, started by a first-page
 * load and discarded by the next refresh; Paging ignores the responses of an invalidated source,
 * so a stale page never merges into a newer list. The feed:
 *
 * - drops a group whose kickoff was already shown in the same generation. The feed's data can
 *   change between pages (a Timeline group can settle and move from the unsettled to the finished
 *   phase), and a kickoff is the list's key, so it must appear once. A refresh shows the group's
 *   current state;
 * - after a rejected continuation, restarts from the first page automatically once, until an
 *   append succeeds or the viewer restarts the list; after that it shows
 *   [KickoffGroupFeedLocalizedException.ListChanged], whose retry restarts the list, so it cannot
 *   loop.
 *
 * An ordinary failed page keeps Paging's own retry, which repeats the failed append with the same
 * continuation however few groups are loaded.
 */
class KickoffGroupFeed(private val query: KickoffGroupQuery) {
    @Volatile
    private var automaticRestartAvailable = true

    /** A restart the viewer asked for (pull to refresh or a list retry) allows one more automatic restart. */
    fun resetAutomaticRestart() {
        automaticRestartAvailable = true
    }

    fun pagingSource(): PagingSource<String, KickoffGroupModel> = GenerationPagingSource()

    private inner class GenerationPagingSource : PagingSource<String, KickoffGroupModel>() {
        // Only Paging's single loading coroutine for this source reads and writes it.
        private var shownKickoffs: Set<Instant> = emptySet()

        override suspend fun load(params: LoadParams<String>): LoadResult<String, KickoffGroupModel> {
            val isAppend = params is LoadParams.Append
            val page = query(params.key).getOrElse { exception -> return failure(exception, isAppend) }
            if (isAppend) automaticRestartAvailable = true
            val unseen = page.items
                .distinctBy { it.kickoffDateTime }
                .filterNot { it.kickoffDateTime in shownKickoffs }
            shownKickoffs = shownKickoffs + unseen.map { it.kickoffDateTime }
            return LoadResult.Page(data = unseen, prevKey = null, nextKey = page.next)
        }

        // Every refresh starts again from the first page.
        override fun getRefreshKey(state: PagingState<String, KickoffGroupModel>): String? = null
    }

    private fun failure(exception: Throwable, isAppend: Boolean): PagingSource.LoadResult<String, KickoffGroupModel> =
        when (exception) {
            is KickoffGroupFeedException.CursorRejected -> cursorRejected(isAppend)
            else -> PagingSource.LoadResult.Error(exception.orDefaultLocalized())
        }

    private fun cursorRejected(isAppend: Boolean): PagingSource.LoadResult<String, KickoffGroupModel> {
        if (isAppend && automaticRestartAvailable) {
            automaticRestartAvailable = false
            // Invalidates this generation; Paging reloads from the first page while the loaded
            // groups stay visible with the refresh indicator.
            return PagingSource.LoadResult.Invalid()
        }
        return PagingSource.LoadResult.Error(KickoffGroupFeedLocalizedException.ListChanged())
    }
}
