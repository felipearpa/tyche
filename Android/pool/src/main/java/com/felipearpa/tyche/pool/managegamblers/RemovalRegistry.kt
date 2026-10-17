package com.felipearpa.tyche.pool.managegamblers

import kotlinx.coroutines.flow.StateFlow

/**
 * The screen-wide record of removal outcomes that each row's [RemoveGamblerViewModel] reports to.
 *
 * A removed gambler stays hidden while the cached pages still contain them; a failed gambler's ID
 * lets a pull-to-refresh release that row. When an ID leaves [failedGamblerIds] while its row
 * still shows the failure, the row returns to idle.
 */
interface RemovalRegistry {
    val failedGamblerIds: StateFlow<Set<String>>

    fun markRemoved(gamblerId: String)

    fun markFailed(gamblerId: String)

    fun clearFailed(gamblerId: String)
}
