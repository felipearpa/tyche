package com.felipearpa.tyche.account

import coil3.annotation.ExperimentalCoilApi
import coil3.fetch.FetchResult
import coil3.network.ConcurrentRequestStrategy
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Runs the fetches of one disk-cache key strictly one after another. Surfaces render the same
 * avatar URL at several sizes, and a replaced photo re-keys all of them at once; fetched
 * concurrently, only one can edit the disk entry and Coil answers every other 304 with a second,
 * full download. Coil's own de-duplicating strategy serializes just the first caller and then
 * releases the rest together.
 */
@ExperimentalCoilApi
class SerialConcurrentRequestStrategy : ConcurrentRequestStrategy {
    private val gatesMutex = Mutex()
    private var gatesByKey: Map<String, Gate> = emptyMap()

    override suspend fun apply(key: String, block: suspend () -> FetchResult): FetchResult {
        val gate = gatesMutex.withLock {
            val gate = gatesByKey[key]?.let { existing -> existing.copy(callers = existing.callers + 1) }
                ?: Gate(mutex = Mutex(), callers = 1)
            gatesByKey = gatesByKey + (key to gate)
            gate
        }

        try {
            return gate.mutex.withLock { block() }
        } finally {
            // Deregistration must survive cancellation, or the key's gate would never be dropped.
            withContext(NonCancellable) {
                gatesMutex.withLock {
                    val remaining = gatesByKey.getValue(key).callers - 1
                    gatesByKey = if (remaining == 0) {
                        gatesByKey - key
                    } else {
                        gatesByKey + (key to gate.copy(callers = remaining))
                    }
                }
            }
        }
    }

    private data class Gate(val mutex: Mutex, val callers: Int)
}
