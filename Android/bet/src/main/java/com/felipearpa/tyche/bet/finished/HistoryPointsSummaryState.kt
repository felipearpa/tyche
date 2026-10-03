package com.felipearpa.tyche.bet.finished

/**
 * The points the signed-in gambler has earned in the current pool, as the server reports them.
 * [Unavailable] is a successful response without a total; it is never treated as zero.
 */
sealed interface HistoryPoints {
    data class Earned(val value: Int) : HistoryPoints
    data object Unavailable : HistoryPoints

    companion object {
        fun of(score: Int?): HistoryPoints = score?.let(::Earned) ?: Unavailable
    }
}

val HistoryPoints.isPositive: Boolean
    get() = this is HistoryPoints.Earned && value > 0

/**
 * History's earned-points summary: the last points the server confirmed and the state of the
 * latest request. The two are independent, so a refresh or a failed refresh keeps the confirmed
 * points on screen.
 */
data class HistoryPointsSummaryState(
    val points: HistoryPoints?,
    val request: Request,
) {
    enum class Request {
        IDLE,

        /** Entry loading or a summary retry. */
        LOADING,

        /** A request started by pull to refresh, whose indicator covers it. */
        PULL_REFRESHING,
        FAILED,
    }

    enum class RefreshStatus { CURRENT, REFRESHING, FAILED }

    sealed interface Presentation {
        /** No points confirmed yet and a request is pending. */
        data object Placeholder : Presentation

        /** No points confirmed yet and the request failed. */
        data object Failed : Presentation

        data class Points(val points: HistoryPoints, val refreshStatus: RefreshStatus) : Presentation
    }

    val presentation: Presentation
        get() {
            val points = points
                ?: return if (request == Request.FAILED) Presentation.Failed else Presentation.Placeholder
            val refreshStatus = when (request) {
                Request.IDLE -> RefreshStatus.CURRENT
                Request.LOADING, Request.PULL_REFRESHING -> RefreshStatus.REFRESHING
                Request.FAILED -> RefreshStatus.FAILED
            }
            return Presentation.Points(points, refreshStatus)
        }

    val isPullRefreshing: Boolean
        get() = request == Request.PULL_REFRESHING

    fun requesting(request: Request) = copy(request = request)

    fun loaded(points: HistoryPoints) = HistoryPointsSummaryState(points = points, request = Request.IDLE)

    fun failed() = copy(request = Request.FAILED)

    companion object {
        val initial = HistoryPointsSummaryState(points = null, request = Request.IDLE)
    }
}
