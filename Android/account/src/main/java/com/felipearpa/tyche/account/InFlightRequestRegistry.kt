package com.felipearpa.tyche.account

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Coalesces concurrent operations by key: the first caller runs [runCoalesced]'s operation in
 * the supervised [scope] while every concurrent caller for the same key awaits that result.
 */
internal class InFlightRequestRegistry<T>(private val scope: CoroutineScope) {
    private val mutex = Mutex()
    private var inFlight: Map<String, Deferred<T>> = emptyMap()

    suspend fun runCoalesced(key: String, operation: suspend () -> T): T {
        val deferred = mutex.withLock {
            inFlight[key] ?: scope.async {
                try {
                    operation()
                } finally {
                    // Deregistration must survive cancellation, or the key would coalesce
                    // onto a dead Deferred forever.
                    withContext(NonCancellable) {
                        mutex.withLock { inFlight = inFlight - key }
                    }
                }
            }.also { created -> inFlight = inFlight + (key to created) }
        }
        return deferred.await()
    }
}
