@file:OptIn(ExperimentalTime::class)

package com.felipearpa.tyche.session

import com.felipearpa.tyche.session.authentication.domain.AuthenticationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

enum class CurrentAccountRefreshTrigger { COLD_START, FOREGROUND, PROFILE_OPENED }

/**
 * Single configuration point for how long a successful current-account validation stays fresh.
 */
val currentAccountFreshness: Duration = 5.minutes

/**
 * Application-scoped coordinator that owns the observable signed-in account.
 *
 * It serializes sign-in installation, refresh, username mutation, and logout clearing so an
 * older remote result can never overwrite a newer local change: every state change happens
 * under one mutex while network calls run outside it, and each in-flight operation is fenced
 * by the epoch it started under. Refresh follows stale-while-revalidate — the persisted
 * snapshot is published immediately, one coalesced request reconciles it when a trigger finds
 * it stale, and a failed refresh retains the snapshot for a later retry.
 */
class CurrentAccountCoordinator internal constructor(
    private val accountStorage: AccountStorage,
    private val authenticationRepository: AuthenticationRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val nowEpochMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val freshness: Duration = currentAccountFreshness,
) {
    private val mutex = Mutex()
    private var snapshot: CurrentAccountSnapshot? = null
    private var epoch = 0
    private var hasRefreshedAfterColdStart = false
    private var hydration: Deferred<Unit>? = null
    private var refreshJob: Deferred<Unit>? = null

    private val mutableState = MutableStateFlow<AccountBundle?>(null)
    val state: StateFlow<AccountBundle?> = mutableState.asStateFlow()

    /**
     * Publishes the persisted snapshot, if any, and returns the current account. Concurrent
     * calls coalesce into one storage read.
     */
    suspend fun hydrate(): AccountBundle? {
        val pending = mutex.withLock {
            hydration ?: scope.async { performHydration() }.also { hydration = it }
        }
        pending.await()
        return state.value
    }

    private suspend fun performHydration() {
        val startEpoch = mutex.withLock { epoch }
        val persisted = runCatching { accountStorage.retrieve() }.getOrNull()
        mutex.withLock {
            if (persisted != null && epoch == startEpoch && snapshot == null) {
                snapshot = persisted
                mutableState.value = persisted.account
            }
        }
    }

    /**
     * Installs a just-signed-in account as freshly validated; sign-in already reconciled it
     * with the server, so the next cold-start refresh is suppressed.
     *
     * A storage failure propagates to the sign-in use case — losing the snapshot silently
     * would sign the user in for one process lifetime only. The write itself cannot be
     * cancelled, so a cancelled caller can never leave a half-installed session on disk.
     */
    suspend fun install(account: AccountBundle) {
        mutex.withLock {
            epoch += 1
            val installed = CurrentAccountSnapshot(
                account = account,
                validatedAtEpochMillis = nowEpochMillis(),
            )
            snapshot = installed
            hasRefreshedAfterColdStart = true
            mutableState.value = account
            persist { accountStorage.store(installed) }
        }
    }

    /**
     * Reconciles the published account with the server when the [trigger] finds it eligible:
     * once after cold start, otherwise only when the last validation is outside [freshness].
     * Concurrent eligible triggers await one shared request.
     */
    suspend fun refresh(trigger: CurrentAccountRefreshTrigger) {
        val pending = mutex.withLock {
            val current = snapshot ?: return
            val eligible = when (trigger) {
                CurrentAccountRefreshTrigger.COLD_START -> !hasRefreshedAfterColdStart
                CurrentAccountRefreshTrigger.FOREGROUND,
                CurrentAccountRefreshTrigger.PROFILE_OPENED,
                -> isStale(current)
            }
            if (!eligible) return
            hasRefreshedAfterColdStart = true
            refreshJob ?: scope.async { performRefresh() }.also { refreshJob = it }
        }
        pending.await()
    }

    private suspend fun performRefresh() {
        try {
            val startEpoch = mutex.withLock { epoch }
            val result = authenticationRepository.getCurrentAccount()
            mutex.withLock {
                result.onSuccess { account ->
                    if (epoch == startEpoch && snapshot != null) {
                        val refreshed = CurrentAccountSnapshot(
                            account = account,
                            validatedAtEpochMillis = nowEpochMillis(),
                        )
                        snapshot = refreshed
                        mutableState.value = account
                        persistQuietly { accountStorage.store(refreshed) }
                    }
                }
                // On failure the snapshot is retained as-is (stale), so a later trigger
                // retries.
            }
        } finally {
            // Even an escaping Throwable must not leave a dead Deferred registered, or every
            // later trigger would await a request that already finished.
            withContext(NonCancellable) { mutex.withLock { refreshJob = null } }
        }
    }

    /**
     * PATCHes the username and, only if no newer operation superseded this one, persists and
     * publishes the submitted username while keeping the current validation time.
     */
    suspend fun updateUsername(username: String): Result<String> {
        val (current, startEpoch) = mutex.withLock { snapshot to epoch }
        if (current == null) {
            return Result.failure(IllegalStateException("No account in storage"))
        }

        return authenticationRepository
            .updateUsername(accountId = current.account.accountId, username = username)
            .map {
                mutex.withLock {
                    val latest = snapshot
                    if (epoch == startEpoch && latest != null) {
                        epoch += 1
                        val updated = latest.copy(account = latest.account.withUsername(username))
                        snapshot = updated
                        mutableState.value = updated.account
                        persistQuietly { accountStorage.store(updated) }
                    }
                }
                username
            }
    }

    /**
     * Clears the published account and its persisted snapshot; results of any still-running
     * refresh or mutation are fenced out and cannot restore the account.
     *
     * A storage failure propagates to the logout use case — silently keeping the snapshot on
     * disk would resurrect the account on the next launch. The delete itself cannot be
     * cancelled, so a caller cancelled mid-logout (e.g. a cleared view model) can no longer
     * leave the snapshot behind.
     */
    suspend fun clear() {
        mutex.withLock {
            epoch += 1
            snapshot = null
            hasRefreshedAfterColdStart = false
            mutableState.value = null
            persist { accountStorage.delete() }
        }
    }

    /** Runs [operation] to completion even if the caller is cancelled; failures propagate. */
    private suspend fun persist(operation: suspend () -> Unit) {
        withContext(NonCancellable) { operation() }
    }

    /**
     * Runs [operation] to completion even if the caller is cancelled, swallowing failures:
     * the server stays authoritative, so the next refresh self-heals a missed write.
     */
    private suspend fun persistQuietly(operation: suspend () -> Unit) {
        withContext(NonCancellable) { runCatching { operation() } }
    }

    private fun isStale(snapshot: CurrentAccountSnapshot): Boolean {
        val validatedAt = snapshot.validatedAtEpochMillis ?: return true
        return nowEpochMillis() - validatedAt >= freshness.inWholeMilliseconds
    }
}
