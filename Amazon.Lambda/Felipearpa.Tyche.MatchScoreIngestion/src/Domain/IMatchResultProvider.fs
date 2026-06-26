namespace Felipearpa.Tyche.MatchScoreIngestion.Domain

/// Fetches a match result from an external sports data provider.
type IMatchResultProvider =

    /// Classifies the match as finished (with the 90-minute result), not yet finished, or abandoned.
    abstract GetMatchPollResultAsync: externalMatchId: string -> Async<MatchPollResult>
