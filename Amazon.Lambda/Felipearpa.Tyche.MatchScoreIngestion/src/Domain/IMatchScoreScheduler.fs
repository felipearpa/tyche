namespace Felipearpa.Tyche.MatchScoreIngestion.Domain

open Felipearpa.Type

/// Manages the per-match polling schedule lifecycle.
type IMatchScoreScheduler =

    /// Creates the recurring schedule that starts 90 minutes after kickoff.
    abstract CreateScheduleAsync: schedule: MatchScoreSchedule -> Async<unit>

    /// Deletes the schedule for a match (idempotent if it no longer exists).
    abstract DeleteScheduleAsync: matchId: Ulid -> Async<unit>
