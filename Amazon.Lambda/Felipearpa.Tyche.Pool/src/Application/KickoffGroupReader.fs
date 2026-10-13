namespace Felipearpa.Tyche.Pool.Application

open System
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

/// Reads the kickoff groups of pool layouts: total and computed match counts per kickoff
/// instant in UTC. One call reads each distinct layout once, so pools that share a layout in
/// a request share the read.
type KickoffGroupReader(layoutMatchReader: IPoolLayoutMatchReader) =

    /// Group membership counts every match of the layout, whatever version added it.
    [<Literal>]
    let everyVersion = Int32.MaxValue

    let rec readMatches (poolLayoutId: Ulid) (next: string option) (acc: ScheduledMatch list) =
        async {
            let! page = layoutMatchReader.GetMatchesThroughVersionAsync(poolLayoutId, everyVersion, next)

            let acc =
                acc
                @ (page.Items
                   |> Seq.map (fun layoutMatch ->
                       { Kickoff = layoutMatch.MatchDateTime
                         HasResult = layoutMatch.Result.IsSome })
                   |> List.ofSeq)

            match page.Next with
            | Some cursor -> return! readMatches poolLayoutId (Some cursor) acc
            | None -> return acc
        }

    let readLayout (poolLayoutId: Ulid) =
        async {
            let! matches = readMatches poolLayoutId None []
            return poolLayoutId, KickoffGroup.ofLayout matches
        }

    /// The kickoff groups of every given layout, keyed by layout.
    member this.ReadAsync(poolLayoutIds: Ulid list) : Async<Map<Ulid, Map<DateTime, KickoffGroup>>> =
        async {
            let! layouts = poolLayoutIds |> List.distinct |> List.map readLayout |> Async.Parallel
            return Map.ofArray layouts
        }
