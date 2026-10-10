package com.felipearpa.tyche.data.bet.domain

/**
 * Failures of the ranked match-bets request that callers handle differently from an ordinary
 * network failure. Authentication, membership, and not-found responses keep the usual
 * `NetworkException`.
 *
 * An [Exception], so the network exception handler passes it through unchanged.
 */
sealed class RankedMatchBetsException : Exception() {
    /**
     * Betting is still open for the match, so other participants' bets stay hidden (409
     * `PREDICTIONS_HIDDEN`). This is a state of the match, not a failure to show.
     */
    class PredictionsHidden : RankedMatchBetsException()

    /**
     * The pool's ranking is not ready or could not be read (503 `RANKING_NOT_READY` or
     * `RANKING_UNAVAILABLE`). Recoverable by retrying later; there is no alternative order.
     */
    class RankingUnavailable : RankedMatchBetsException()

    /**
     * The server rejected the continuation (400 `INVALID_CURSOR` or `CURSOR_CONTEXT_MISMATCH`).
     * The feed restarts from its first page.
     */
    class CursorRejected : RankedMatchBetsException()

    companion object {
        /** The ranked failure for an error response, or `null` when it is not one of them. */
        fun from(statusCode: Int, errorCode: String?): RankedMatchBetsException? =
            when (statusCode to errorCode) {
                409 to "PREDICTIONS_HIDDEN" -> PredictionsHidden()
                503 to "RANKING_NOT_READY", 503 to "RANKING_UNAVAILABLE" -> RankingUnavailable()
                400 to "INVALID_CURSOR", 400 to "CURSOR_CONTEXT_MISMATCH" -> CursorRejected()
                else -> null
            }
    }
}
