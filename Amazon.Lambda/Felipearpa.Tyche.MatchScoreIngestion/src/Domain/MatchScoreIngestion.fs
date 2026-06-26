namespace Felipearpa.Tyche.MatchScoreIngestion.Domain

open System
open Felipearpa.Type

/// The 90-minute result of a finished match, ready to be written onto a pool layout match.
type FinishedMatchResult =
    { HomeTeamScore: int
      AwayTeamScore: int }

/// Outcome of a single poll against the external provider.
type MatchPollResult =
    /// The match is over; carries the 90-minute result.
    | Finished of FinishedMatchResult
    /// The match has not finished yet; keep polling.
    | NotYetFinished
    /// The match will never produce a result (cancelled, postponed, awarded); give up.
    | Abandoned

/// Everything needed to register a per-match polling schedule.
type MatchScoreSchedule =
    { MatchId: Ulid
      PoolLayoutId: Ulid
      ExternalMatchId: string
      MatchDateTime: DateTime }
