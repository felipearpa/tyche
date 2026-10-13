namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Core.Paging
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module private TimelineCursor =

    [<Literal>]
    let liveTag = "live:"

    [<Literal>]
    let finishedTag = "finished:"

    type Phase =
        | Live
        | Finished

    let parse (maybeCursor: string option) : Phase * string option =
        match maybeCursor with
        | None -> Live, None
        | Some cursor when cursor.StartsWith(liveTag) ->
            let inner = cursor.Substring(liveTag.Length)
            Live, (if System.String.IsNullOrEmpty inner then None else Some inner)
        | Some cursor when cursor.StartsWith(finishedTag) ->
            let inner = cursor.Substring(finishedTag.Length)
            Finished, (if System.String.IsNullOrEmpty inner then None else Some inner)
        | Some cursor -> Live, Some cursor

    let formatLive (next: string option) : string option =
        next |> Option.map (fun n -> liveTag + n)

    let formatFinished (next: string option) : string option =
        next |> Option.map (fun n -> finishedTag + n)

/// Another gambler's Timeline as kickoff groups in two phases. Unsettled groups (some layout
/// match has no result) come first, oldest kickoff first, with every revealed row of the group,
/// computed or not. Then settled groups, newest kickoff first. A group belongs to exactly one
/// phase by its layout counts, so it appears once, and a page holds whole groups. Each phase's
/// storage traversal is exhausted, through empty pages, before the next phase starts.
type GetGamblerBetsTimeline(poolGamblerBetRepository: IPoolGamblerBetRepository, kickoffGroupReader: KickoffGroupReader) =

    let unsettledPage (poolId: Ulid) (gamblerId: Ulid) (cursor: string option) =
        async {
            let! page = poolGamblerBetRepository.GetUnsettledBetsByKickoffAsync(poolId, gamblerId, cursor)
            let runs = KickoffGroupFeed.runs (List.ofSeq page.Items)
            let! layoutGroups = GroupedBetFeed.layoutGroupsOf kickoffGroupReader page.Items

            let isUnsettled (kickoff, _) =
                KickoffGroupFeed.phase (layoutGroups |> KickoffGroup.at kickoff) = UnsettledGroup

            // One read per group brings its computed rows along with the revealed pending ones
            // and completes a group the storage page cut short.
            let! groups =
                runs
                |> List.filter isUnsettled
                |> List.map (fun (kickoff, members) ->
                    async {
                        let! whole, after =
                            poolGamblerBetRepository.GetBetsAtKickoffAsync(poolId, gamblerId, (List.last members).StoredKickoff)

                        return kickoff, (if whole.IsEmpty then members else whole), after
                    })
                |> Async.Sequential

            let next =
                match page.Next, List.tryLast runs, Array.tryLast groups with
                | Some _, Some(lastKickoff, _), Some(kickoff, _, after) when kickoff = lastKickoff ->
                    after |> Option.orElse page.Next
                | next, _, _ -> next

            return
                { Items =
                    groups
                    |> Array.map (fun (kickoff, children, _) -> KickoffGroupFeed.item layoutGroups kickoff children)
                    |> List.ofArray
                  Next = next }
        }

    let settledPage (poolId: Ulid) (gamblerId: Ulid) (cursor: string option) =
        async {
            let! page = poolGamblerBetRepository.GetFinishedBetsByKickoffAsync(poolId, gamblerId, cursor)

            let! runs, next =
                GroupedBetFeed.completeLastGroup
                    (fun storedKickoff ->
                        poolGamblerBetRepository.GetFinishedBetsAtKickoffAsync(poolId, gamblerId, storedKickoff))
                    page

            let! layoutGroups = GroupedBetFeed.layoutGroupsOf kickoffGroupReader page.Items

            return
                { Items =
                    runs
                    |> List.map (fun (kickoff, children) -> KickoffGroupFeed.item layoutGroups kickoff children)
                    |> List.filter (fun item -> KickoffGroupFeed.phase item.Group = SettledGroup)
                  Next = next }
        }

    member this.ExecuteAsync(poolId: Ulid, gamblerId: Ulid, maybeCursor: string option) =
        async {
            let phase, innerCursor = TimelineCursor.parse maybeCursor

            match phase with
            | TimelineCursor.Live ->
                let! unsettled = unsettledPage poolId gamblerId innerCursor

                match unsettled.Next with
                | Some _ ->
                    return
                        { CursorPage.Items = unsettled.Items
                          Next = TimelineCursor.formatLive unsettled.Next }
                | None ->
                    let! settled = settledPage poolId gamblerId None

                    return
                        { CursorPage.Items = Seq.append unsettled.Items settled.Items
                          Next = TimelineCursor.formatFinished settled.Next }
            | TimelineCursor.Finished ->
                let! settled = settledPage poolId gamblerId innerCursor

                return
                    { CursorPage.Items = settled.Items
                      Next = TimelineCursor.formatFinished settled.Next }
        }
