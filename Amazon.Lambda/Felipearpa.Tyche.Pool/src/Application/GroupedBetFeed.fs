namespace Felipearpa.Tyche.Pool.Application

open System
open Felipearpa.Core.Paging
open Felipearpa.Tyche.Pool.Domain

/// Steps shared by the grouped History and Timeline feeds.
module internal GroupedBetFeed =

    /// The kickoff groups of the layout the details belong to; a gambler's details in one pool
    /// share it. A page without details reads no layout.
    let layoutGroupsOf (kickoffGroupReader: KickoffGroupReader) (bets: GroupedBet seq) =
        async {
            match bets |> Seq.tryHead with
            | None -> return Map.empty<DateTime, KickoffGroup>
            | Some first ->
                let layoutId = first.Bet.PoolLayoutId
                let! layouts = kickoffGroupReader.ReadAsync [ layoutId ]
                return layouts |> Map.find layoutId
        }

    /// Splits a storage page into kickoff runs and, when the storage traversal continues, reads
    /// the last run's whole group so a page never splits a group. The continuation then resumes
    /// after that group. If the equality read finds nothing (the index has not caught up), the
    /// page is returned as read.
    let completeLastGroup
        (readWholeGroup: string -> Async<GroupedBet list * string option>)
        (page: GroupedBet CursorPage)
        : Async<(DateTime * GroupedBet list) list * string option> =
        async {
            let runs = KickoffGroupFeed.runs (List.ofSeq page.Items)

            match page.Next, List.rev runs with
            | Some _, (kickoff, members) :: earlier ->
                let! whole, after = readWholeGroup (List.last members).StoredKickoff

                if whole.IsEmpty then
                    return runs, page.Next
                else
                    return List.rev ((kickoff, whole) :: earlier), after |> Option.orElse page.Next
            | _ -> return runs, page.Next
        }
