package com.felipearpa.tyche.bet.finished

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.felipearpa.tyche.bet.kickoffgroup.KickoffGroupFeed
import com.felipearpa.tyche.bet.kickoffgroup.KickoffGroupModel
import com.felipearpa.tyche.data.bet.application.GetFinishedPoolGamblerBets
import com.felipearpa.tyche.data.pool.application.GetPoolGamblerScore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * The signed-in gambler's History in one pool: the grouped feed of their computed matches, newest
 * kickoff first, and their authoritative earned-points total. The two load independently, so
 * either can succeed while the other is loading or failed.
 */
class FinishedBetListViewModel(
    private val poolId: String,
    val gamblerId: String,
    private val getFinishedPoolGamblerBets: GetFinishedPoolGamblerBets,
    getPoolGamblerScore: GetPoolGamblerScore,
) : ViewModel() {

    /** How many production group placeholders fill the first load. */
    val placeholderCount = PLACEHOLDER_GROUP_COUNT

    private val feed = KickoffGroupFeed(
        query = { next ->
            getFinishedBetsPagingQuery(
                next = next,
                poolId = poolId,
                gamblerId = gamblerId,
                getFinishedPoolGamblerBets = getFinishedPoolGamblerBets,
            )
        },
    )

    /** The kickoff groups in the server's order; see [KickoffGroupFeed]. */
    val kickoffGroups: StateFlow<PagingData<KickoffGroupModel>> = buildPager().flow
        .cachedIn(scope = viewModelScope)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = PagingData.empty(),
        )

    private val pointsSummaryLoader = HistoryPointsSummaryLoader(
        scope = viewModelScope,
        poolId = poolId,
        gamblerId = gamblerId,
        getPoolGamblerScore = getPoolGamblerScore,
    )

    /** The authoritative total for this view model's pool and gambler; see [HistoryPointsSummaryLoader]. */
    val pointsSummary: StateFlow<HistoryPointsSummaryState> = pointsSummaryLoader.state

    init {
        loadPointsSummary()
    }

    /** Requests only the total, as a summary retry does; the groups are not reloaded. */
    fun loadPointsSummary() {
        pointsSummaryLoader.load()
    }

    /**
     * A pull to refresh: requests the total and allows the groups, which restart from their first
     * page by themselves, one more automatic restart.
     */
    fun refresh() {
        feed.resetAutomaticRestart()
        pointsSummaryLoader.refresh()
    }

    /** Called before the groups restart from their first page at the viewer's request. */
    fun onFeedRestart() {
        feed.resetAutomaticRestart()
    }

    private fun buildPager() =
        Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = feed::pagingSource,
        )
}

private const val PAGE_SIZE = 20
private const val PREFETCH_DISTANCE = 3
private const val PLACEHOLDER_GROUP_COUNT = 4
