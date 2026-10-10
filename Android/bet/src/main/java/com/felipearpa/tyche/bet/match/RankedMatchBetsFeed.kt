package com.felipearpa.tyche.bet.match

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.data.bet.domain.RankedMatchBetsException
import com.felipearpa.tyche.ui.exception.orDefaultLocalized

typealias RankedMatchBetsQuery = suspend (next: String?) -> Result<CursorPage<PoolGamblerBetModel>>

/**
 * The paging rules of one ranked participant feed. The server's order is final, so pages are never
 * sorted here. Each [pagingSource] is one generation, started by a first-page load and discarded
 * by the next refresh; Paging ignores the responses of an invalidated source. The feed:
 *
 * - drops participants already shown in the same generation, because totals that change while the
 *   viewer pages can move a participant across a page boundary;
 * - fetches the next page at once when deduplication leaves a page empty, a bounded number of
 *   times;
 * - after a rejected continuation, restarts from the first page automatically once, until an
 *   append succeeds or the viewer restarts the list; after that it shows
 *   [MatchBetsLocalizedException.ListChanged], whose retry restarts the list, so it cannot loop;
 * - reports a hidden-predictions answer through [onPredictionsHidden] instead of showing it as a
 *   failure.
 */
class RankedMatchBetsFeed(
    private val query: RankedMatchBetsQuery,
    private val onPredictionsHidden: () -> Unit,
) {
    @Volatile
    private var automaticRestartAvailable = true

    /** A restart the viewer asked for (pull to refresh or a list retry) allows one more automatic restart. */
    fun resetAutomaticRestart() {
        automaticRestartAvailable = true
    }

    fun pagingSource(): PagingSource<String, PoolGamblerBetModel> = GenerationPagingSource()

    private inner class GenerationPagingSource : PagingSource<String, PoolGamblerBetModel>() {
        // Only Paging's single loading coroutine for this source reads and writes it.
        private var seenGamblerIds: Set<String> = emptySet()

        override suspend fun load(params: LoadParams<String>): LoadResult<String, PoolGamblerBetModel> {
            val isAppend = params is LoadParams.Append
            var cursor = params.key
            repeat(MAXIMUM_EMPTY_PAGE_REFILLS + 1) {
                val page = query(cursor).getOrElse { exception -> return failure(exception, isAppend) }
                if (isAppend) automaticRestartAvailable = true
                val unseen = page.items
                    .distinctBy { it.gamblerId }
                    .filterNot { it.gamblerId in seenGamblerIds }
                seenGamblerIds = seenGamblerIds + unseen.map { it.gamblerId }
                val following = page.next
                if (unseen.isNotEmpty() || following == null) {
                    return LoadResult.Page(data = unseen, prevKey = null, nextKey = following)
                }
                cursor = following
            }
            return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = cursor)
        }

        // Every refresh starts again from the first page.
        override fun getRefreshKey(state: PagingState<String, PoolGamblerBetModel>): String? = null
    }

    private fun failure(exception: Throwable, isAppend: Boolean): PagingSource.LoadResult<String, PoolGamblerBetModel> =
        when (exception) {
            is RankedMatchBetsException.PredictionsHidden -> {
                onPredictionsHidden()
                PagingSource.LoadResult.Error(exception)
            }

            is RankedMatchBetsException.CursorRejected -> cursorRejected(isAppend)
            is RankedMatchBetsException.RankingUnavailable ->
                PagingSource.LoadResult.Error(MatchBetsLocalizedException.RankingUnavailable())

            else -> PagingSource.LoadResult.Error(exception.orDefaultLocalized())
        }

    private fun cursorRejected(isAppend: Boolean): PagingSource.LoadResult<String, PoolGamblerBetModel> {
        if (isAppend && automaticRestartAvailable) {
            automaticRestartAvailable = false
            // Invalidates this generation; Paging reloads from the first page.
            return PagingSource.LoadResult.Invalid()
        }
        return PagingSource.LoadResult.Error(MatchBetsLocalizedException.ListChanged())
    }
}

/** How many times one page request continues past a page that deduplication left empty. */
private const val MAXIMUM_EMPTY_PAGE_REFILLS = 3
