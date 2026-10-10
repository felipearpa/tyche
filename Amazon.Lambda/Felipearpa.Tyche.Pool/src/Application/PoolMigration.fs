namespace Felipearpa.Tyche.Pool.Application

open System
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module private PoolMigrationPaging =

    [<Literal>]
    let contendedAttempts = 8

    /// Retries a guarded write while it meets transaction contention, which DynamoDB reports
    /// for transactions that touch an item another transaction is still finishing.
    let rec retryContended (write: unit -> Async<GuardedWriteOutcome>) attempt =
        async {
            match! write () with
            | Contended when attempt + 1 < contendedAttempts ->
                do! Async.Sleep(Random.Shared.Next(25, 50 * (1 <<< min attempt 5)))
                return! retryContended write (attempt + 1)
            | Contended ->
                return raise (InvalidOperationException "A guarded migration write kept meeting transaction contention")
            | outcome -> return outcome
        }

    [<Literal>]
    let memberPageSize = 25

    [<Literal>]
    let detailPageSize = 100

    let rec allDetails (repository: IPoolProjectionRepository) poolId gamblerId cursor acc =
        async {
            let! page, next = repository.GetBetProjectionPageAsync(poolId, gamblerId, cursor, detailPageSize)
            let acc = acc @ page

            match next with
            | Some _ -> return! allDetails repository poolId gamblerId next acc
            | None -> return acc
        }

    let rec allMembers (repository: IPoolProjectionRepository) poolId cursor acc =
        async {
            let! page, next = repository.GetMembershipPageAsync(poolId, cursor, memberPageSize)
            let acc = acc @ page

            match next with
            | Some _ -> return! allMembers repository poolId next acc
            | None -> return acc
        }

type IdentityInitializationReport =
    { Members: int
      BoundDetails: int
      /// Gamblers whose details are ambiguous (for example retained from an earlier membership)
      /// and stay unbound for repair.
      HeldMembers: Ulid list }

/// Gives existing memberships a generation and initial score version, then binds their
/// existing details to it. A membership's details are bound only when the awards of its
/// computed details add up to its total; otherwise some details may belong to an earlier
/// membership of the same gambler, and the whole membership is held for repair instead of
/// being bound by guesswork.
type InitializePoolIdentities(repository: IPoolProjectionRepository, maintenancePublisher: IPoolMaintenancePublisher) =

    let ensureGeneration poolId (membership: MembershipState) =
        async {
            match membership.Generation with
            | Some _ -> return Some membership
            | None ->
                let generation = Ulid.random().Value

                let! _ =
                    PoolMigrationPaging.retryContended
                        (fun () -> repository.InitializeMembershipIdentityAsync(poolId, membership.GamblerId, generation))
                        0

                return! repository.GetMembershipAsync(poolId, membership.GamblerId)
        }

    let isConsistent (membership: MembershipState) (generation: string) (details: BetProjectionState list) =
        let counted =
            details
            |> List.filter (fun bet -> bet.IsComputed && (bet.Generation.IsNone || bet.Generation = Some generation))
            |> List.sumBy (fun bet -> bet.Award |> Option.defaultValue 0)

        membership.Total = Some counted

    let bind poolId (membership: MembershipState) =
        async {
            match! ensureGeneration poolId membership with
            | None -> return 0, false
            | Some current ->
                let generation = current.Generation.Value
                let! details = PoolMigrationPaging.allDetails repository poolId current.GamblerId None []
                let unbound = details |> List.filter (fun bet -> bet.Generation.IsNone)

                if unbound.IsEmpty then
                    return 0, false
                elif not (isConsistent current generation details) then
                    return 0, true
                else
                    let! outcomes =
                        unbound
                        |> List.map (fun bet ->
                            PoolMigrationPaging.retryContended
                                (fun () ->
                                    repository.BindDetailIdentityAsync(
                                        poolId,
                                        current.GamblerId,
                                        bet.MatchId,
                                        generation,
                                        bet.IsComputed
                                    ))
                                0)
                        |> Async.Sequential

                    return outcomes |> Array.filter ((=) Written) |> Array.length, false
        }

    member this.ExecuteAsync(poolId: Ulid, afterGamblerId: string option) : Async<IdentityInitializationReport> =
        async {
            let! page, next = repository.GetMembershipPageAsync(poolId, afterGamblerId, PoolMigrationPaging.memberPageSize)
            let! results = page |> List.map (bind poolId) |> Async.Sequential

            if next.IsSome then
                do! maintenancePublisher.PublishAsync [ InitializeIdentities(poolId, next) ]

            return
                { Members = page.Length
                  BoundDetails = results |> Array.sumBy fst
                  HeldMembers =
                    Array.zip (Array.ofList page) results
                    |> Array.filter (fun (_, (_, held)) -> held)
                    |> Array.map (fun (membership, _) -> membership.GamblerId)
                    |> List.ofArray }
        }

/// Initializes the pool-total projection on existing details through the guarded
/// initializer, a few memberships per job, continuing through a durable continuation.
/// Restarting is safe: initialized details are skipped and newer projections are never
/// overwritten.
type BackfillPoolProjection(repository: IPoolProjectionRepository, maintenancePublisher: IPoolMaintenancePublisher) =

    [<Literal>]
    let concurrency = 4

    [<Literal>]
    let bindingAttempts = 6

    let needsProjection (binding: MembershipBinding) (bet: BetProjectionState) =
        bet.Generation = Some binding.Generation
        && (not bet.Active
            || not bet.HasRankingKey
            || bet.ScoreVersion |> Option.forall (fun version -> version < binding.ScoreVersion))

    let rec initialize poolId gamblerId (bet: BetProjectionState) (binding: MembershipBinding) attempt =
        async {
            let write () = repository.InitializeProjectionAsync(poolId, gamblerId, bet.MatchId, binding)

            match! PoolMigrationPaging.retryContended write 0 with
            | Written -> return 1
            | _ when attempt + 1 >= bindingAttempts ->
                return raise (InvalidOperationException $"Projection backfill for {gamblerId.Value} did not converge")
            | _ ->
                let! membership = repository.GetMembershipAsync(poolId, gamblerId)

                match membership |> Option.bind MembershipBinding.tryOf with
                | Some current when current.Generation = binding.Generation ->
                    if current.ScoreVersion = binding.ScoreVersion then
                        // The detail already carries this or a newer projection.
                        return 0
                    else
                        return! initialize poolId gamblerId bet current (attempt + 1)
                | _ -> return 0
        }

    let backfillMember poolId (membership: MembershipState) =
        async {
            match MembershipBinding.tryOf membership with
            | None -> return 0
            | Some binding ->
                let! details = PoolMigrationPaging.allDetails repository poolId membership.GamblerId None []

                // One member's writes all check the same membership, so they run one at a time;
                // concurrent transactions on one item would only contend.
                let! counts =
                    details
                    |> List.filter (needsProjection binding)
                    |> List.map (fun bet -> initialize poolId membership.GamblerId bet binding 0)
                    |> Async.Sequential

                return Array.sum counts
        }

    /// Returns the number of details initialized by this job.
    member this.ExecuteAsync(poolId: Ulid, afterGamblerId: string option) : Async<int> =
        async {
            let! page, next = repository.GetMembershipPageAsync(poolId, afterGamblerId, PoolMigrationPaging.memberPageSize)
            let! counts = Async.Parallel(page |> List.map (backfillMember poolId), concurrency)

            if next.IsSome then
                do! maintenancePublisher.PublishAsync [ BackfillProjection(poolId, next) ]

            return Array.sum counts
        }

/// Checks that every current membership's details carry a valid, current projection and
/// records the pool's ranking readiness. Any unbound detail, inactive or stale projection,
/// or uninitialized membership keeps the pool not ready. The result is stored only if the
/// membership set did not change during the check.
type VerifyPoolRankingReadiness(settlementRepository: IPoolSettlementRepository, repository: IPoolProjectionRepository) =

    let unresolvedFor (membership: MembershipState) (details: BetProjectionState list) =
        match MembershipBinding.tryOf membership with
        | None -> details.Length + 1
        | Some binding ->
            details
            |> List.filter (fun bet ->
                match bet.Generation with
                | None -> true
                | Some generation when generation = binding.Generation ->
                    not bet.Active
                    || not bet.HasRankingKey
                    || bet.ScoreVersion <> Some binding.ScoreVersion
                    || bet.Total <> Some binding.Total
                | Some _ -> bet.Active || bet.HasRankingKey)
            |> List.length

    member this.ExecuteAsync(poolId: Ulid) : Async<RankingReadinessReport option> =
        async {
            match! settlementRepository.GetRootAsync poolId with
            | None -> return None
            | Some root ->
                let! members = PoolMigrationPaging.allMembers repository poolId None []

                let! perMember =
                    members
                    |> List.map (fun membership ->
                        async {
                            let! details = PoolMigrationPaging.allDetails repository poolId membership.GamblerId None []
                            return details.Length, unresolvedFor membership details
                        })
                    |> Async.Sequential

                let unresolved = perMember |> Array.sumBy snd

                let report =
                    { Readiness = (if unresolved = 0 then Ready else NotReady)
                      CheckedMembers = members.Length
                      CheckedDetails = perMember |> Array.sumBy fst
                      Unresolved = unresolved }

                let! stored = repository.SetReadinessAsync(poolId, report, root.MembershipRevision)

                return
                    if stored then
                        Some report
                    else
                        Some { report with Readiness = NotReady }
        }
