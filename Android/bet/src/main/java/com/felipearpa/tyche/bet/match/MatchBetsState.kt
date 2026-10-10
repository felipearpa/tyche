package com.felipearpa.tyche.bet.match

import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.core.type.TeamScore

/**
 * What the match screen shows about other participants' bets, from the server's lock and
 * computation flags. Computation takes precedence over the lock flag. None of these states claims
 * that play is live: the lock happens before kickoff and computation can be delayed.
 */
enum class MatchBetsPresentation {
    /** Betting is open, so only the viewer's own bet is shown. */
    HIDDEN,

    /** Betting is closed and points are not computed: predictions without awards. */
    BETS_CLOSED,

    /** Points are computed: predictions with this match's awards. */
    COMPUTED;

    val revealsParticipants: Boolean
        get() = this != HIDDEN

    companion object {
        /**
         * [predictionsHiddenByServer] is set when the ranked feed answered that predictions are
         * hidden; the server's privacy gate wins over every flag the heading carries.
         */
        fun of(match: PoolGamblerBetModel, predictionsHiddenByServer: Boolean = false): MatchBetsPresentation =
            when {
                predictionsHiddenByServer -> HIDDEN
                match.isComputed -> COMPUTED
                match.isLocked -> BETS_CLOSED
                else -> HIDDEN
            }
    }
}

/**
 * The match heading and its privacy gate: the last match the server confirmed and the state of the
 * latest request. They are independent, so a refresh or a failed refresh keeps the confirmed
 * heading on screen.
 */
data class MatchHeadingState(
    val match: PoolGamblerBetModel?,
    val request: Request,
    /**
     * Set when the ranked feed answers that predictions are hidden; cleared when a later heading
     * load confirms the match again, so the feed is asked again.
     */
    val predictionsHiddenByServer: Boolean,
) {
    enum class Request {
        IDLE,

        /** Entry, reappearance, or a retry. */
        LOADING,

        /** A request started by pull to refresh, whose indicator covers it. */
        PULL_REFRESHING,
        FAILED,
    }

    enum class RefreshStatus { CURRENT, REFRESHING, FAILED }

    sealed interface Presentation {
        /** No match confirmed yet and a request is pending: the gate is unknown. */
        data object Placeholder : Presentation

        /** No match confirmed yet and the request failed. */
        data object Failed : Presentation

        data class Loaded(
            val match: PoolGamblerBetModel,
            val presentation: MatchBetsPresentation,
            val refreshStatus: RefreshStatus,
        ) : Presentation
    }

    val presentation: Presentation
        get() {
            val match = match
                ?: return if (request == Request.FAILED) Presentation.Failed else Presentation.Placeholder
            val refreshStatus = when (request) {
                Request.IDLE -> RefreshStatus.CURRENT
                Request.LOADING, Request.PULL_REFRESHING -> RefreshStatus.REFRESHING
                Request.FAILED -> RefreshStatus.FAILED
            }
            return Presentation.Loaded(
                match = match,
                presentation = MatchBetsPresentation.of(match, predictionsHiddenByServer),
                refreshStatus = refreshStatus,
            )
        }

    /** The confirmed state of other participants' bets, or `null` while the gate is unknown. */
    val confirmedPresentation: MatchBetsPresentation?
        get() = (presentation as? Presentation.Loaded)?.presentation

    val isPullRefreshing: Boolean
        get() = request == Request.PULL_REFRESHING

    fun requesting(request: Request) = copy(request = request)

    /**
     * A freshly confirmed match. It clears a server-reported hidden gate: an open match is hidden by
     * its own flags, and a closed one asks the feed again.
     */
    fun loaded(match: PoolGamblerBetModel) =
        MatchHeadingState(match = match, request = Request.IDLE, predictionsHiddenByServer = false)

    fun failed() = copy(request = Request.FAILED)

    fun hiddenByServer() = copy(predictionsHiddenByServer = true)

    companion object {
        val initial = MatchHeadingState(match = null, request = Request.IDLE, predictionsHiddenByServer = false)
    }
}

/**
 * The signed-in viewer's own prediction for the match, shown while bets are hidden. Loading, a
 * failed request, and a confirmed absence stay distinct.
 */
sealed interface OwnBetState {
    data object Loading : OwnBetState
    data class Bet(val betScore: TeamScore<Int>) : OwnBetState
    data object NoBet : OwnBetState
    data object Failed : OwnBetState

    val isConfirmed: Boolean
        get() = this is Bet || this == NoBet

    companion object {
        fun of(betScore: TeamScore<Int>?): OwnBetState = betScore?.let(::Bet) ?: NoBet
    }
}

/**
 * What sits between the teams. An uncomputed score is only the score reported so far; a missing
 * computed result shows dashes, never an invented 0–0.
 */
sealed interface MatchHeaderCenter {
    data object Versus : MatchHeaderCenter
    data class MatchScore(val score: TeamScore<Int>) : MatchHeaderCenter
    data class FinalScore(val score: TeamScore<Int>?) : MatchHeaderCenter

    companion object {
        fun of(match: PoolGamblerBetModel, presentation: MatchBetsPresentation): MatchHeaderCenter =
            when (presentation) {
                MatchBetsPresentation.COMPUTED -> FinalScore(match.matchScore)
                MatchBetsPresentation.BETS_CLOSED -> match.matchScore?.let(::MatchScore) ?: Versus
                MatchBetsPresentation.HIDDEN -> Versus
            }
    }
}
