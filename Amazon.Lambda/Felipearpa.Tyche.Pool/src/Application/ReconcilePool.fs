namespace Felipearpa.Tyche.Pool.Application

open System
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type PoolReconciliationReport =
    { Members: int
      MaterializedDetails: int
      UnsettledMatches: Ulid list
      StaleProjections: int
      UninitializedProjections: int
      RetiredGenerations: int
      StalePositions: int
      /// Memberships with retained details of an earlier generation that were neither restored
      /// nor held for repair, for which a restoration was requested.
      RequestedRestorations: int
      /// Retained details held for repair against their membership's current generation.
      HeldRetainedDetails: int }

type private MemberInspection =
    { Membership: MembershipState
      Missing: PoolLayoutMatchSnapshot list
      Unsettled: Ulid list
      StaleProjections: int
      Uninitialized: int
      OtherGenerations: BetProjectionState list
      AwaitsRestoration: bool
      HeldRetained: int }

/// Finds work that delivery failures, expired retention, or index lag left undone for a page
/// of memberships, and repairs it through the normal guarded and durable paths: missing
/// details are materialized through the guarded initializer, matches whose result a detail
/// does not reflect get a match reconciliation, lagging totals get propagation,
/// uninitialized projections get a backfill, active details of another generation are
/// retired, memberships behind the last completed operation get a standings reconciliation,
/// and memberships with retained details of an earlier generation that are neither restored
/// nor held for repair get their rejoin restoration. It reads authoritative state; a failed read raises instead of being
/// treated as zero, unavailable, or missing.
type ReconcilePool
    (
        settlementRepository: IPoolSettlementRepository,
        projectionRepository: IPoolProjectionRepository,
        betRepository: IPoolGamblerBetRepository,
        layoutMatchReader: IPoolLayoutMatchReader,
        commandPublisher: IPoolCommandPublisher,
        maintenancePublisher: IPoolMaintenancePublisher
    ) =

    [<Literal>]
    let memberPageSize = 25

    let rec allMatches poolLayoutId next acc =
        async {
            let! page = layoutMatchReader.GetMatchesThroughVersionAsync(poolLayoutId, Int32.MaxValue, next)
            let acc = acc @ List.ofSeq page.Items

            match page.Next with
            | Some _ -> return! allMatches poolLayoutId page.Next acc
            | None -> return acc
        }

    let rec allDetails poolId gamblerId cursor acc =
        async {
            let! page, next = projectionRepository.GetBetProjectionPageAsync(poolId, gamblerId, cursor, 100)
            let acc = acc @ page

            match next with
            | Some _ -> return! allDetails poolId gamblerId next acc
            | None -> return acc
        }

    let inspect poolId (matches: PoolLayoutMatchSnapshot list) (membership: MembershipState) =
        async {
            let! details = allDetails poolId membership.GamblerId None []
            let present = details |> List.map _.MatchId |> Set.ofList
            let withResult = matches |> List.filter _.Result.IsSome |> List.map _.MatchId |> Set.ofList
            let binding = MembershipBinding.tryOf membership

            let own (bet: BetProjectionState) =
                binding |> Option.exists (fun b -> bet.Generation = Some b.Generation)

            // Only details bound to an earlier generation are retained from an earlier
            // membership for certain: an unbound computed detail can also belong to a membership
            // that identity initialization held for repair, whose total may already count it.
            let awaitsRestoration (bet: BetProjectionState) =
                binding
                |> Option.exists (fun b ->
                    bet.IsComputed
                    && bet.Generation.IsSome
                    && bet.Generation <> Some b.Generation
                    && bet.RestoreExcludedGeneration <> Some b.Generation)

            let heldRetained (bet: BetProjectionState) =
                binding
                |> Option.exists (fun b -> bet.Generation <> Some b.Generation && bet.RestoreExcludedGeneration = Some b.Generation)

            let stale (bet: BetProjectionState) =
                binding
                |> Option.exists (fun b -> own bet && bet.Active && bet.ScoreVersion |> Option.exists (fun v -> v < b.ScoreVersion))

            return
                { Membership = membership
                  Missing = matches |> List.filter (fun m -> not (present.Contains m.MatchId))
                  Unsettled =
                    details
                    |> List.filter (fun bet -> withResult.Contains bet.MatchId && not bet.IsComputed)
                    |> List.map _.MatchId
                  StaleProjections = details |> List.filter stale |> List.length
                  Uninitialized = details |> List.filter (fun bet -> own bet && not bet.Active) |> List.length
                  OtherGenerations =
                    details
                    |> List.filter (fun bet -> bet.Generation.IsSome && not (own bet) && (bet.Active || bet.HasRankingKey))
                  AwaitsRestoration = details |> List.exists awaitsRestoration
                  HeldRetained = details |> List.filter heldRetained |> List.length }
        }

    let materializeMissing poolId (inspection: MemberInspection) =
        async {
            let gambler =
                { PoolLayoutGambler.PoolId = poolId
                  PoolLayoutId = inspection.Membership.PoolLayoutId
                  PoolLayoutVersion = 0
                  GamblerId = inspection.Membership.GamblerId
                  GamblerUsername = NonEmptyString100.newOf inspection.Membership.GamblerUsername
                  GamblerEmail = NonEmptyString100.newOf inspection.Membership.GamblerEmail }

            let! results =
                inspection.Missing
                |> List.map (fun layoutMatch ->
                    BetMaterialization.materialize
                        betRepository
                        projectionRepository
                        (BetMaterialization.uncomputedBet gambler layoutMatch)
                        0)
                |> Async.Sequential

            return results |> Array.filter ((=) Created) |> Array.length
        }

    let retireOtherGenerations poolId (inspection: MemberInspection) =
        async {
            for bet in inspection.OtherGenerations do
                let! _ =
                    projectionRepository.DeactivateRankingAsync(poolId, inspection.Membership.GamblerId, bet.MatchId, bet.Generation.Value)

                ()
        }

    let stalePositions (root: PoolSettlementRoot) (page: MembershipState list) =
        page
        |> List.filter (fun membership ->
            membership.PositionOperationRevision
            |> Option.forall (fun revision -> revision < root.LastCompletedOperationRevision))
        |> List.length

    let restorations poolId (inspections: MemberInspection list) =
        inspections
        |> List.filter _.AwaitsRestoration
        |> List.choose (fun inspection ->
            inspection.Membership.Generation
            |> Option.map (PoolCommand.restoreRejoinHistory poolId inspection.Membership.GamblerId))

    let publish poolId source next (inspections: MemberInspection list) unsettled stale =
        async {
            let commands =
                (unsettled |> List.map (fun matchId -> PoolCommand.reconcileMatch poolId matchId source))
                @ (if stale > 0 then [ PoolCommand.reconcileStandings poolId source ] else [])
                @ restorations poolId inspections

            let jobs =
                (inspections
                 |> List.filter (fun inspection -> inspection.StaleProjections > 0)
                 |> List.map (fun inspection -> PropagateTotal(poolId, inspection.Membership.GamblerId, None)))
                @ (if inspections |> List.exists (fun inspection -> inspection.Uninitialized > 0) then
                       [ BackfillProjection(poolId, None) ]
                   else
                       [])
                @ (next |> Option.map (fun cursor -> PoolMaintenanceJob.ReconcilePool(poolId, Some cursor)) |> Option.toList)

            if not commands.IsEmpty then
                do! commandPublisher.PublishAsync commands

            if not jobs.IsEmpty then
                do! maintenancePublisher.PublishAsync jobs
        }

    let empty =
        { Members = 0
          MaterializedDetails = 0
          UnsettledMatches = []
          StaleProjections = 0
          UninitializedProjections = 0
          RetiredGenerations = 0
          StalePositions = 0
          RequestedRestorations = 0
          HeldRetainedDetails = 0 }

    /// Reconciles one page of memberships and continues through a durable continuation. The
    /// source names the reconciliation run so its match reconciliations get stable identities.
    member this.ExecuteAsync(poolId: Ulid, afterGamblerId: string option, source: string) : Async<PoolReconciliationReport> =
        async {
            match! settlementRepository.GetRootAsync poolId with
            | None -> return empty
            | Some root ->
                let! matches = allMatches root.PoolLayoutId None []
                let! page, next = projectionRepository.GetMembershipPageAsync(poolId, afterGamblerId, memberPageSize)
                let! inspections = page |> List.map (inspect poolId matches) |> Async.Sequential
                let inspections = List.ofArray inspections
                let! materialized = inspections |> List.map (materializeMissing poolId) |> Async.Sequential

                for inspection in inspections do
                    do! retireOtherGenerations poolId inspection

                let missingWithResult =
                    inspections
                    |> List.collect (fun inspection -> inspection.Missing |> List.filter _.Result.IsSome |> List.map _.MatchId)

                let unsettled =
                    (inspections |> List.collect _.Unsettled) @ missingWithResult |> List.distinct

                let stale = if root.LastCompletedOperationRevision > 0L then stalePositions root page else 0
                do! publish poolId source next inspections unsettled stale

                return
                    { Members = page.Length
                      MaterializedDetails = Array.sum materialized
                      UnsettledMatches = unsettled
                      StaleProjections = inspections |> List.sumBy _.StaleProjections
                      UninitializedProjections = inspections |> List.sumBy _.Uninitialized
                      RetiredGenerations = inspections |> List.sumBy (fun inspection -> inspection.OtherGenerations.Length)
                      StalePositions = stale
                      RequestedRestorations = restorations poolId inspections |> List.length
                      HeldRetainedDetails = inspections |> List.sumBy _.HeldRetained }
        }
