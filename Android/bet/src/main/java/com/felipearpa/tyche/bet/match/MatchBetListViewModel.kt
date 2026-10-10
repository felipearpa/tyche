package com.felipearpa.tyche.bet.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.felipearpa.network.HttpStatus
import com.felipearpa.network.NetworkException
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.toPoolGamblerBetModel
import com.felipearpa.tyche.core.paging.map
import com.felipearpa.tyche.data.bet.application.GetPoolGamblerBet
import com.felipearpa.tyche.data.bet.application.GetRankedPoolMatchGamblerBets
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The match screen for one signed-in account, pool, and match. Another context gets another view
 * model, so an earlier context's responses never reach it.
 *
 * It keeps three independent parts:
 * - the heading and privacy gate, from the routed gambler's bet for the match;
 * - the viewer's own prediction, shown while bets are hidden. It always belongs to
 *   [viewerAccountId], never to the routed gambler, who can be another participant;
 * - the ranked participant feed, which exists only while the gate confirms bets are revealed.
 *
 * Heading and own-bet requests carry generations; a response from an older request is discarded.
 * Feed pages of an older paging generation are discarded by Paging itself.
 */
class MatchBetListViewModel(
    private val poolId: String,
    private val routedGamblerId: String,
    private val matchId: String,
    val viewerAccountId: String,
    private val getPoolGamblerBet: GetPoolGamblerBet,
    private val getRankedPoolMatchGamblerBets: GetRankedPoolMatchGamblerBets,
) : ViewModel() {

    val pageSize = PAGE_SIZE

    private val _heading = MutableStateFlow(MatchHeadingState.initial)
    val heading: StateFlow<MatchHeadingState> = _heading.asStateFlow()

    private val _ownBet = MutableStateFlow<OwnBetState>(OwnBetState.Loading)
    val ownBet: StateFlow<OwnBetState> = _ownBet.asStateFlow()

    private var headingGeneration = 0
    private var ownBetGeneration = 0

    private val feed = RankedMatchBetsFeed(
        query = { next ->
            getRankedPoolMatchGamblerBets.execute(poolId = poolId, matchId = matchId, next = next)
                .map { page -> page.map { bet -> bet.toPoolGamblerBetModel() } }
        },
        // Paging loads off the main thread; state changes happen on it.
        onPredictionsHidden = { viewModelScope.launch { predictionsHiddenByServer() } },
    )

    /**
     * The ranked participants. A new pager starts each time the gate confirms that bets are
     * revealed, so rows from an earlier revealed period never reappear before a fresh first page.
     * While the gate is unknown or hidden there is no pager and nothing is requested.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val participantBets: Flow<PagingData<PoolGamblerBetModel>> = _heading
        .map { heading -> heading.confirmedPresentation?.revealsParticipants == true }
        .distinctUntilChanged()
        .flatMapLatest { revealed -> if (revealed) buildPager().flow else flowOf(loadingPagingData()) }
        .cachedIn(viewModelScope)

    /**
     * Entry and reappearance: confirms the gate, then loads the own prediction when bets are hidden.
     * The participant list loads itself once it is shown.
     */
    fun load() {
        viewModelScope.launch {
            loadHeading(MatchHeadingState.Request.LOADING)
            if (_heading.value.confirmedPresentation == MatchBetsPresentation.HIDDEN) loadOwnBet()
        }
    }

    /**
     * Pull to refresh: rechecks the gate, then refreshes the own prediction when bets are hidden.
     * The participant list restarts from its first page by itself (see `RefreshableLazyPagingColumn`)
     * and replaces its rows when that page arrives.
     */
    fun refresh() {
        feed.resetAutomaticRestart()
        viewModelScope.launch {
            loadHeading(MatchHeadingState.Request.PULL_REFRESHING)
            if (_heading.value.confirmedPresentation == MatchBetsPresentation.HIDDEN) loadOwnBet()
        }
    }

    /** The own prediction's retry; requests only that prediction. */
    fun retryOwnBet() {
        viewModelScope.launch { loadOwnBet() }
    }

    /** Called before the participant list restarts from its first page at the viewer's request. */
    fun onFeedRestart() {
        feed.resetAutomaticRestart()
    }

    private suspend fun loadHeading(request: MatchHeadingState.Request) {
        val generation = ++headingGeneration
        _heading.update { it.requesting(request) }

        val result = getPoolGamblerBet.execute(poolId = poolId, gamblerId = routedGamblerId, matchId = matchId)
        if (generation != headingGeneration) return

        result
            .onSuccess { bet ->
                val match = bet.toPoolGamblerBetModel()
                _heading.update { it.loaded(match) }
                if (routedGamblerId == viewerAccountId) {
                    ownBetGeneration++
                    _ownBet.value = OwnBetState.of(match.betScore)
                }
            }
            .onFailure { _heading.update { it.failed() } }
    }

    /**
     * Requests the viewer's own prediction. When the route already belongs to the viewer, the
     * heading's bet is the viewer's and no other request is made.
     */
    private suspend fun loadOwnBet() {
        if (routedGamblerId == viewerAccountId) {
            if (_heading.value.match == null) loadHeading(MatchHeadingState.Request.LOADING)
            return
        }
        val generation = ++ownBetGeneration
        val confirmed = _ownBet.value.isConfirmed
        if (!confirmed) _ownBet.value = OwnBetState.Loading

        val result = getPoolGamblerBet.execute(poolId = poolId, gamblerId = viewerAccountId, matchId = matchId)
        if (generation != ownBetGeneration) return

        result
            .onSuccess { bet -> _ownBet.value = OwnBetState.of(bet.betScore) }
            .onFailure { exception ->
                when {
                    // The server has no prediction record for the viewer in this match.
                    exception.isNotFound -> _ownBet.value = OwnBetState.NoBet
                    // A failed refresh keeps the confirmed prediction.
                    !confirmed -> _ownBet.value = OwnBetState.Failed
                }
            }
    }

    private suspend fun predictionsHiddenByServer() {
        _heading.update { it.hiddenByServer() }
        loadOwnBet()
    }

    private fun buildPager() =
        Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = feed::pagingSource,
        )
}

/**
 * Stands in while there is no pager: nothing loaded and nothing complete, which the shared paging
 * list shows as its loading placeholders rather than as an empty list.
 */
private fun loadingPagingData(): PagingData<PoolGamblerBetModel> =
    PagingData.empty(
        sourceLoadStates = LoadStates(
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            prepend = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.NotLoading(endOfPaginationReached = false),
        ),
    )

private val Throwable.isNotFound: Boolean
    get() = this is NetworkException.Http && httpStatus == HttpStatus.NOT_FOUND

private const val PAGE_SIZE = 50
private const val PREFETCH_DISTANCE = 5
