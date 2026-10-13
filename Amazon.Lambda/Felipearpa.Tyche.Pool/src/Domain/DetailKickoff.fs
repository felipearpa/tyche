namespace Felipearpa.Tyche.Pool.Domain

open System
open System.Globalization
open Felipearpa.Type

/// The kickoff copies a match detail holds: `matchDateTime`, which orders the pending feed,
/// and `computedMatchDateTime`, which orders the finished feed once the detail is computed.
type DetailKickoff =
    { PoolId: Ulid
      GamblerId: Ulid
      MatchId: Ulid
      /// As stored.
      MatchDateTime: string
      /// As stored; None while the detail is pending, or computed before the copy existed.
      ComputedMatchDateTime: string option
      IsComputed: bool }

/// A rewrite of a detail's kickoff copies, guarded by the copies it was planned from.
type KickoffCopyWrite =
    { Expected: DetailKickoff
      MatchDateTime: string
      ComputedMatchDateTime: string option }

[<RequireQualifiedAccess>]
module DetailKickoff =

    /// The stored form of a kickoff: ISO-8601 in UTC, as details are written.
    let storedForm (kickoff: DateTime) =
        (KickoffGroup.instant kickoff).ToString("o", CultureInfo.InvariantCulture)

    let private instantOf (stored: string) =
        DateTime.Parse(stored, CultureInfo.InvariantCulture, DateTimeStyles.RoundtripKind)
        |> KickoffGroup.instant

    let private changed (detail: DetailKickoff) (matchDateTime: string) (computedMatchDateTime: string option) =
        if matchDateTime = detail.MatchDateTime && computedMatchDateTime = detail.ComputedMatchDateTime then
            None
        else
            Some
                { Expected = detail
                  MatchDateTime = matchDateTime
                  ComputedMatchDateTime = computedMatchDateTime }

    /// Moves every copy that still holds the previous kickoff to the new one. A copy holding
    /// any other instant, including the new one, is left as it is, so a repeated or stale
    /// change writes nothing.
    let reschedule (previous: DateTime) (kickoff: DateTime) (detail: DetailKickoff) : KickoffCopyWrite option =
        let target = storedForm kickoff
        let previous = KickoffGroup.instant previous
        let move (stored: string) = if instantOf stored = previous then target else stored

        changed detail (move detail.MatchDateTime) (detail.ComputedMatchDateTime |> Option.map move)

    /// Copies the kickoff onto a computed detail that lacks its finished-feed copy, as
    /// settlement does on a first computation. Pending details and details that have the copy
    /// are left as they are.
    let backfill (detail: DetailKickoff) : KickoffCopyWrite option =
        match detail.IsComputed, detail.ComputedMatchDateTime with
        | true, None -> changed detail detail.MatchDateTime (Some detail.MatchDateTime)
        | _ -> None
