package com.felipearpa.tyche.session

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Persisted cache envelope for the signed-in account.
 *
 * [validatedAtEpochMillis] records the last successful server validation. `null` means the
 * snapshot was never validated — either it predates validation (a migrated legacy bundle) or
 * it has not been reconciled yet — and is therefore treated as stale.
 */
@Serializable
data class CurrentAccountSnapshot(
    val account: AccountBundle,
    val validatedAtEpochMillis: Long? = null,
)

private val tolerantJson = Json { ignoreUnknownKeys = true }

/**
 * Decodes a persisted current-account payload, accepting both the envelope format and the
 * legacy raw [AccountBundle] that released versions persisted. A legacy bundle migrates as a
 * never-validated (stale) snapshot so existing signed-in gamblers are not logged out. An
 * unrecognizable payload yields `null` instead of throwing.
 */
internal fun decodeCurrentAccountSnapshot(raw: String): CurrentAccountSnapshot? =
    runCatching { tolerantJson.decodeFromString<CurrentAccountSnapshot>(raw) }
        .recoverCatching {
            CurrentAccountSnapshot(
                account = tolerantJson.decodeFromString<AccountBundle>(raw),
                validatedAtEpochMillis = null,
            )
        }
        .getOrNull()
