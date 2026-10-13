namespace Felipearpa.Tyche.Pool.Domain

open System

/// The matches of a pool's layout that share one scheduled kickoff instant.
type KickoffGroup =
    { /// The shared kickoff instant, in UTC. It identifies the group within the pool.
      KickoffDateTime: DateTime
      TotalMatches: int
      /// Layout matches of the group that hold a result, whether or not the pool finished
      /// settling it.
      ComputedMatches: int }

/// A layout match as kickoff grouping sees it.
type ScheduledMatch = { Kickoff: DateTime; HasResult: bool }

[<RequireQualifiedAccess>]
module KickoffGroup =

    /// The group key of a kickoff: the same instant in UTC, without rounding. Stored instants
    /// carry their offset; one read without a kind is UTC by the storage convention.
    let instant (kickoff: DateTime) =
        match kickoff.Kind with
        | DateTimeKind.Unspecified -> DateTime.SpecifyKind(kickoff, DateTimeKind.Utc)
        | _ -> kickoff.ToUniversalTime()

    /// Groups one layout's matches by kickoff instant.
    let ofLayout (matches: ScheduledMatch seq) : Map<DateTime, KickoffGroup> =
        matches
        |> Seq.groupBy (fun scheduled -> instant scheduled.Kickoff)
        |> Seq.map (fun (kickoff, members) ->
            kickoff,
            { KickoffDateTime = kickoff
              TotalMatches = Seq.length members
              ComputedMatches = members |> Seq.filter _.HasResult |> Seq.length })
        |> Map.ofSeq

    /// The group at the kickoff. A kickoff that no layout match has any longer, such as one
    /// whose match was rescheduled, is a group without matches.
    let at (kickoff: DateTime) (groups: Map<DateTime, KickoffGroup>) =
        let key = instant kickoff

        groups
        |> Map.tryFind key
        |> Option.defaultValue
            { KickoffDateTime = key
              TotalMatches = 0
              ComputedMatches = 0 }
