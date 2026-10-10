namespace Felipearpa.Tyche.Pool.Application

open System
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

/// The verified outcome of settling one membership's match detail.
type DetailSettlementOutcome =
    | Settled
    | AlreadySettled
    /// Deliberately excluded after verification: the membership was removed or replaced.
    | Excluded
    /// The membership exists but its detail does not yet; materialization requests repair.
    | PendingMaterialization
    /// Identity metadata is missing or values contradict each other; held for repair.
    | Held of reason: string

type PositionsPassResult =
    | PositionsApplied of unresolved: int
    /// Membership, totals, or markers changed under the calculation; recalculate.
    | PositionsInvalidated
    /// The invocation deadline arrived before every position was written.
    | PositionsInterrupted

/// Applies one result version to a page of authoritative memberships.
module SettlementPass =

    let rec private settleMember
        (repository: IPoolSettlementRepository)
        (settings: PoolOperationSettings)
        (poolId: Ulid)
        (active: ActiveOperation)
        (matchId: Ulid)
        (result: MatchResult)
        (now: unit -> DateTime)
        (attempt: int)
        (membership: MembershipState)
        (bet: BetSettlementState option)
        : Async<DetailSettlementOutcome> =
        async {
            match bet with
            | None -> return PendingMaterialization
            | Some bet ->
                match SettlementPolicy.decide membership bet result with
                | AlreadyApplied -> return AlreadySettled
                | ObsoleteMembership -> return Excluded
                | IdentityUnbound -> return Held "identity unbound"
                | Malformed reason -> return Held reason
                | Apply change ->
                    let write =
                        { SettlementWrite.PoolId = poolId
                          GamblerId = membership.GamblerId
                          MatchId = matchId
                          OperationId = active.OperationId
                          OperationRevision = active.Revision
                          Result = result
                          Change = change
                          ComputedAt = now () }

                    match! repository.ApplySettlementAsync write with
                    | Written -> return Settled
                    | outcome ->
                        if attempt + 1 >= settings.GuardedWriteAttempts then
                            return
                                raise (
                                    InvalidOperationException
                                        $"Settlement of {membership.GamblerId.Value} for {active.OperationId} did not converge: {outcome}"
                                )
                        else
                            if outcome = Contended then
                                do! GuardedWriteRetry.backoff attempt

                            return!
                                retry repository settings poolId active matchId result now (attempt + 1) membership.GamblerId
        }

    /// Re-reads both records after a failed condition so the outcome is classified from
    /// persisted state rather than assumed from the exception.
    and private retry repository settings poolId active matchId result now attempt gamblerId =
        async {
            let! membership = repository.GetMembershipAsync(poolId, gamblerId)

            match membership with
            | None -> return Excluded
            | Some membership ->
                let! bet = repository.GetBetSettlementStateAsync(poolId, gamblerId, matchId)
                return! settleMember repository settings poolId active matchId result now attempt membership bet
        }

    let run
        (repository: IPoolSettlementRepository)
        (settings: PoolOperationSettings)
        (poolId: Ulid)
        (active: ActiveOperation)
        (matchId: Ulid)
        (result: MatchResult)
        (now: unit -> DateTime)
        (page: MembershipState list)
        =
        async {
            let! bets =
                repository.GetBetSettlementStatesAsync(poolId, matchId, page |> List.map _.GamblerId)

            let betsByGambler = bets |> List.map (fun bet -> bet.GamblerId, bet) |> Map.ofList

            let! outcomes =
                Async.Parallel(
                    page
                    |> List.map (fun membership ->
                        settleMember
                            repository
                            settings
                            poolId
                            active
                            matchId
                            result
                            now
                            0
                            membership
                            (betsByGambler |> Map.tryFind membership.GamblerId)),
                    settings.WriteConcurrency
                )

            return outcomes |> List.ofArray
        }

/// Recalculates positions from strongly consistent membership reads and writes them with
/// per-operation markers.
module PositionsPass =

    type private PlannedWrite =
        { Membership: MembershipState
          Write: PositionWrite }

    let rec private readAll (repository: IPoolSettlementRepository) settings poolId cursor acc =
        async {
            let! page, next = repository.GetMembershipPageAsync(poolId, cursor, settings.MembershipPageSize)
            let acc = acc @ page

            match next with
            | Some next -> return! readAll repository settings poolId (Some next) acc
            | None -> return acc
        }

    let private targetMatch (active: ActiveOperation) =
        match active.Target with
        | MatchSettlement(matchId, _) -> Some matchId
        | CurrentStandings
        | RejoinRestoration _ -> None

    let private plan (poolId: Ulid) (active: ActiveOperation) (pairedDetails: Map<Ulid, string option>) ranked =
        ranked
        |> List.choose (fun (membership: MembershipState, position) ->
            match membership.Generation, membership.ScoreVersion with
            | Some generation, Some scoreVersion ->
                match PositionPolicy.decide active.Target active.Revision membership position with
                | WritePosition change ->
                    let target =
                        targetMatch active
                        |> Option.filter (fun _ -> pairedDetails |> Map.tryFind membership.GamblerId = Some(Some generation))

                    Some
                        { Membership = membership
                          Write =
                            { PoolId = poolId
                              GamblerId = membership.GamblerId
                              Generation = generation
                              ExpectedScoreVersion = scoreVersion
                              ExpectedPositionOperationRevision = membership.PositionOperationRevision
                              OperationRevision = active.Revision
                              CalculationRevision = active.CalculationRevision
                              Change = change
                              TargetMatchId = target } }
                | PositionCurrent
                | PositionSuperseded -> None
            | _ -> None)

    /// Writes one planned position. Returns false when the membership changed under the
    /// calculation; a detail that cannot take the pair is written membership-only.
    let rec private write (repository: IPoolSettlementRepository) settings (planned: PlannedWrite) attempt =
        async {
            match! repository.ApplyPositionAsync planned.Write with
            | Written -> return true, false
            | Contended when attempt + 1 < settings.GuardedWriteAttempts ->
                do! Async.Sleep(Random.Shared.Next(10, 20 * (attempt + 1)))
                return! write repository settings planned (attempt + 1)
            | Contended -> return false, false
            | ConditionNotMet ->
                let! current = repository.GetMembershipAsync(planned.Write.PoolId, planned.Write.GamblerId)

                let unchanged =
                    current
                    |> Option.exists (fun membership ->
                        membership.Generation = planned.Membership.Generation
                        && membership.ScoreVersion = planned.Membership.ScoreVersion
                        && membership.PositionOperationRevision = planned.Membership.PositionOperationRevision
                        && membership.Position = planned.Membership.Position)

                match unchanged, planned.Write.TargetMatchId with
                | true, Some _ ->
                    let membershipOnly = { planned with Write = { planned.Write with TargetMatchId = None } }
                    let! written, _ = write repository settings membershipOnly attempt
                    return written, true
                | _ -> return false, false
        }

    let run
        (repository: IPoolSettlementRepository)
        (settings: PoolOperationSettings)
        (poolId: Ulid)
        (active: ActiveOperation)
        (deadline: DateTime)
        (now: unit -> DateTime)
        =
        async {
            let! members = readAll repository settings poolId None []
            let! root = repository.GetRootAsync poolId

            match root with
            | Some root when root.MembershipRevision = active.MembershipRevision ->
                let! paired =
                    match targetMatch active with
                    | Some matchId ->
                        async {
                            let! bets =
                                repository.GetBetSettlementStatesAsync(poolId, matchId, members |> List.map _.GamblerId)

                            return bets |> List.map (fun bet -> bet.GamblerId, bet.Generation) |> Map.ofList
                        }
                    | None -> async { return Map.empty }

                let planned = PositionPolicy.rank members |> plan poolId active paired

                let rec writeChunks (chunks: PlannedWrite list list) unresolved =
                    async {
                        match chunks with
                        | [] -> return PositionsApplied unresolved
                        | _ when now () >= deadline -> return PositionsInterrupted
                        | chunk :: rest ->
                            let! outcomes =
                                Async.Parallel(
                                    chunk |> List.map (fun planned -> write repository settings planned 0),
                                    settings.WriteConcurrency
                                )

                            if outcomes |> Array.forall fst then
                                let degraded = outcomes |> Array.filter snd |> Array.length
                                return! writeChunks rest (unresolved + degraded)
                            else
                                return PositionsInvalidated
                    }

                // Details missing or excluded here were already counted by the settlement pass;
                // only pairs a present detail rejected are new unresolved work.
                return! writeChunks (planned |> List.chunkBySize settings.MembershipPageSize) 0
            | _ -> return PositionsInvalidated
        }
