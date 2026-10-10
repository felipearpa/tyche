namespace Felipearpa.Tyche.Pool.Application

open System
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

/// A committed membership change observed on the Pool stream.
type MembershipChange =
    /// A join; the generation is the new membership's, absent only on records written before
    /// generations existed.
    | MembershipCreated of poolId: Ulid * gamblerId: Ulid * generation: string option
    | MembershipRemoved of poolId: Ulid * gamblerId: Ulid
    /// The authoritative total or its identity changed (a new score version or generation).
    | MembershipTotalChanged of poolId: Ulid * gamblerId: Ulid

/// Turns committed membership changes into durable work: current-standings reconciliation
/// for joins and removals, a rejoin restoration for every join, and total propagation for new
/// score versions. A join that turns out to have no retained details completes its restoration
/// without changes. Position and projection writes never produce a total change, so they
/// cannot trigger themselves.
type ReactToMembershipChanges(commandPublisher: IPoolCommandPublisher, maintenancePublisher: IPoolMaintenancePublisher) =

    member this.ExecuteAsync(changes: (MembershipChange * string) list) : Async<unit> =
        let standings =
            changes
            |> List.choose (fun (change, source) ->
                match change with
                | MembershipCreated(poolId, _, _)
                | MembershipRemoved(poolId, _) -> Some(poolId, source)
                | MembershipTotalChanged _ -> None)
            // One reconciliation per pool and delivery, named after the pool's last event.
            |> List.groupBy fst
            |> List.map (fun (poolId, events) -> PoolCommand.reconcileStandings poolId (events |> List.last |> snd))

        let restorations =
            changes
            |> List.choose (fun (change, _) ->
                match change with
                | MembershipCreated(poolId, gamblerId, Some generation) ->
                    Some(PoolCommand.restoreRejoinHistory poolId gamblerId generation)
                | _ -> None)
            |> List.distinct

        let propagation =
            changes
            |> List.choose (fun (change, _) ->
                match change with
                | MembershipTotalChanged(poolId, gamblerId) -> Some(PropagateTotal(poolId, gamblerId, None))
                | _ -> None)
            |> List.distinct

        async {
            let commands = standings @ restorations

            if not commands.IsEmpty then
                do! commandPublisher.PublishAsync commands

            if not propagation.IsEmpty then
                do! maintenancePublisher.PublishAsync propagation
        }

/// Retires the removed generation's projection. Uncomputed details of that generation are
/// deleted as before; computed details keep their data under the existing retention rules
/// but lose their ranking keys, and their generation tombstone stops any later work from
/// reactivating them. Details of any other generation, such as a rejoined membership's,
/// are never touched.
type CleanUpRemovedMembership(projectionRepository: IPoolProjectionRepository) =

    [<Literal>]
    let pageSize = 100

    [<Literal>]
    let concurrency = 8

    let rec retire poolId gamblerId (generation: string option) (bet: BetProjectionState) attempt =
        async {
            let write =
                match generation, bet.Generation, bet.IsComputed with
                | Some removed, Some current, false when removed = current ->
                    Some(projectionRepository.DeleteUncomputedAsync(poolId, gamblerId, bet.MatchId, generation))
                | Some removed, Some current, true when removed = current && (bet.Active || bet.HasRankingKey) ->
                    Some(projectionRepository.DeactivateRankingAsync(poolId, gamblerId, bet.MatchId, removed))
                // A removal recorded before generations existed only touches unbound details.
                | None, None, false -> Some(projectionRepository.DeleteUncomputedAsync(poolId, gamblerId, bet.MatchId, None))
                | _ -> None

            match write with
            | None -> return ()
            | Some write ->
                match! write with
                | Contended when attempt < 5 ->
                    do! Async.Sleep(20 * (attempt + 1))
                    return! retire poolId gamblerId generation bet (attempt + 1)
                | Contended -> return raise (InvalidOperationException $"Cleanup of {gamblerId.Value} kept contending")
                // An uncomputed detail that was settled meanwhile is retired as computed.
                | ConditionNotMet when not bet.IsComputed && generation.IsSome && attempt = 0 ->
                    return! retire poolId gamblerId generation { bet with IsComputed = true; Active = true } (attempt + 1)
                | ConditionNotMet
                | Written -> return ()
        }

    member this.ExecuteAsync(poolId: Ulid, gamblerId: Ulid, generation: string option) : Async<unit> =
        let rec loop (cursor: string option) =
            async {
                let! page, next = projectionRepository.GetBetProjectionPageAsync(poolId, gamblerId, cursor, pageSize)

                do!
                    Async.Parallel(page |> List.map (fun bet -> retire poolId gamblerId generation bet 0), concurrency)
                    |> Async.Ignore

                match next with
                | Some _ -> return! loop next
                | None -> return ()
            }

        loop None

/// Copies a membership's latest absolute total and score version onto its active match
/// details of the same generation. Superseded jobs read the latest total, so they converge
/// to it instead of replaying increments; stale and duplicate writes fail their version
/// guard and change nothing. Long partitions continue through a durable continuation.
type PropagateMembershipTotal(projectionRepository: IPoolProjectionRepository, maintenancePublisher: IPoolMaintenancePublisher) =

    [<Literal>]
    let pageSize = 100

    [<Literal>]
    let pagesPerJob = 5

    [<Literal>]
    let concurrency = 8

    let rec apply poolId gamblerId binding (bet: BetProjectionState) attempt =
        async {
            match! projectionRepository.ApplyTotalAsync(poolId, gamblerId, bet.MatchId, binding) with
            | Written -> return 1
            | ConditionNotMet -> return 0
            | Contended when attempt < 5 ->
                do! Async.Sleep(20 * (attempt + 1))
                return! apply poolId gamblerId binding bet (attempt + 1)
            | Contended ->
                return raise (InvalidOperationException $"Total propagation for {gamblerId.Value} kept contending")
        }

    let needsTotal (binding: MembershipBinding) (bet: BetProjectionState) =
        bet.Generation = Some binding.Generation
        && bet.Active
        && bet.ScoreVersion |> Option.exists (fun version -> version < binding.ScoreVersion)

    /// Returns the number of details updated in this job.
    member this.ExecuteAsync(poolId: Ulid, gamblerId: Ulid, afterMatchId: string option) : Async<int> =
        let rec loop binding cursor pagesLeft updated =
            async {
                if pagesLeft = 0 then
                    do! maintenancePublisher.PublishAsync [ PropagateTotal(poolId, gamblerId, cursor) ]
                    return updated
                else
                    let! page, next = projectionRepository.GetBetProjectionPageAsync(poolId, gamblerId, cursor, pageSize)

                    let! counts =
                        Async.Parallel(
                            page |> List.filter (needsTotal binding) |> List.map (fun bet -> apply poolId gamblerId binding bet 0),
                            concurrency
                        )

                    let updated = updated + Array.sum counts

                    match next with
                    | Some _ -> return! loop binding next (pagesLeft - 1) updated
                    | None -> return updated
            }

        async {
            let! membership = projectionRepository.GetMembershipAsync(poolId, gamblerId)

            match membership |> Option.bind MembershipBinding.tryOf with
            | None -> return 0
            | Some binding -> return! loop binding afterMatchId pagesPerJob 0
        }
