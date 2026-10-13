package com.felipearpa.tyche.data.bet.domain

/**
 * Failures of the grouped History and Timeline feeds that callers handle differently from an
 * ordinary network failure. Other error responses keep the usual `NetworkException`.
 *
 * An [Exception], so the network exception handler passes it through unchanged.
 */
sealed class KickoffGroupFeedException : Exception() {
    /**
     * The server rejected the continuation (400 `INVALID_CURSOR` or `CURSOR_CONTEXT_MISMATCH`),
     * for example one issued before the grouped feeds. The feed restarts from its first page.
     */
    class CursorRejected : KickoffGroupFeedException()

    companion object {
        /** The grouped-feed failure for an error response, or `null` when it is not one of them. */
        fun from(statusCode: Int, errorCode: String?): KickoffGroupFeedException? =
            when (statusCode to errorCode) {
                400 to "INVALID_CURSOR", 400 to "CURSOR_CONTEXT_MISMATCH" -> CursorRejected()
                else -> null
            }
    }
}
