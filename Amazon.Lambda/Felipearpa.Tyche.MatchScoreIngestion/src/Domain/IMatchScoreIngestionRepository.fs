namespace Felipearpa.Tyche.MatchScoreIngestion.Domain

open Felipearpa.Type

/// Data access for the match-score ingestion control row.
type IMatchScoreIngestionRepository =

    /// Records that the schedule fired: stamps the last poll time and increments the poll count.
    abstract RecordPollAsync: matchId: Ulid -> Async<unit>

    /// Marks the row as completed and stamps when the score became effective in the pool layout.
    abstract MarkCompletedAsync: matchId: Ulid -> Async<unit>

    /// Marks the row as expired: the match was abandoned or never finished within the cutoff window.
    abstract MarkExpiredAsync: matchId: Ulid -> Async<unit>
