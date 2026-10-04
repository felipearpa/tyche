package com.felipearpa.tyche.bet.finished

import com.felipearpa.tyche.data.pool.application.GetPoolGamblerScore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The authoritative earned-points total of one gambler in one pool, as History and Timeline show
 * it. It is requested on entry, on pull to refresh, and on summary retry; paging never requests
 * or derives it. One loader serves one pool and gambler, so another context gets a new loader
 * and never sees this total. Requests run in [scope], which ends with the owning view model.
 */
class HistoryPointsSummaryLoader(
    private val scope: CoroutineScope,
    private val poolId: String,
    private val gamblerId: String,
    private val getPoolGamblerScore: GetPoolGamblerScore,
) {
    private val _state = MutableStateFlow(HistoryPointsSummaryState.initial)

    val state: StateFlow<HistoryPointsSummaryState> = _state.asStateFlow()

    private var job: Job? = null

    /** Identifies the latest request; a response from an older request is discarded. */
    private var generation = 0

    /**
     * Requests the total on its own, as entry and a summary retry do, so it never reloads the
     * rows. Confirmed points stay visible while it runs.
     */
    fun load() {
        request(HistoryPointsSummaryState.Request.LOADING)
    }

    /**
     * Requests the total for a pull to refresh, which also refreshes the rows. The pull indicator
     * stays while this request is pending.
     */
    fun refresh() {
        request(HistoryPointsSummaryState.Request.PULL_REFRESHING)
    }

    private fun request(request: HistoryPointsSummaryState.Request) {
        job?.cancel()
        val requestGeneration = ++generation
        _state.update { state -> state.requesting(request) }

        job = scope.launch {
            val result = getPoolGamblerScore.execute(poolId = poolId, gamblerId = gamblerId)
            // A superseded request can still answer when its call ignores cancellation.
            if (requestGeneration != generation) return@launch
            _state.update { state ->
                result.fold(
                    onSuccess = { score -> state.loaded(HistoryPoints.of(score.score)) },
                    onFailure = { state.failed() },
                )
            }
        }
    }
}
