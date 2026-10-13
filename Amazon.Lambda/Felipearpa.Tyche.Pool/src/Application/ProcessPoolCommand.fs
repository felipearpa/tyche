namespace Felipearpa.Tyche.Pool.Application

open System
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type PoolCommandOutcome =
    /// The command's operation finished both stages, or needed no work.
    | OperationCompleted
    /// The operation's receipt was already completed; the delivery is a duplicate or replay.
    | AlreadyCompleted
    /// Progress was checkpointed before the deadline, or another worker owns the operation.
    /// Deliver the same command again.
    | Yielded
    /// The pool's active operation is blocked; this command is held outside the fence.
    | DeferredWhileBlocked
    /// This command's own operation is blocked and needs operator repair.
    | Blocked
    | PoolNotFound

type PoolCommandReport =
    { Outcome: PoolCommandOutcome
      /// The operation worked on during this invocation, which can be an earlier active one.
      ActiveOperationId: string option
      Settled: int
      Unresolved: int
      ActiveSince: DateTime option
      /// How long the command should stay invisible before it is delivered again: the rest of
      /// the lease another worker holds. None means at once.
      RetryAfter: TimeSpan option }

/// Runs one ordered pool operation per command on the per-pool FIFO queue: admission with a
/// durable receipt, fenced checkpoints, settlement or rejoin restoration, authoritative
/// positions, and atomic completion. A later command first helps the active operation finish and never overtakes it.
type ProcessPoolCommand
    (
        repository: IPoolSettlementRepository,
        layoutMatchReader: IPoolLayoutMatchReader,
        commandPublisher: IPoolCommandPublisher,
        settings: PoolOperationSettings,
        now: unit -> DateTime
    ) =

    let newToken () = Ulid.random().Value

    let report outcome (active: ActiveOperation option) settled =
        { Outcome = outcome
          ActiveOperationId = active |> Option.map _.OperationId
          Settled = settled
          Unresolved = active |> Option.map _.UnresolvedDetails |> Option.defaultValue 0
          ActiveSince = active |> Option.map _.AdmittedAt
          RetryAfter = None }

    /// Yields to the worker holding the lease; the command comes back once that lease ends.
    let yieldUntil (leaseExpiresAt: DateTime) (active: ActiveOperation) settled =
        { report Yielded (Some active) settled with
            RetryAfter = Some(max TimeSpan.Zero (leaseExpiresAt - now ())) }

    /// Yields after a fence write found that another worker owns the operation now. The wait
    /// is the rest of that worker's lease, read from the root.
    let yieldToOwner (poolId: Ulid) (active: ActiveOperation) settled =
        async {
            let! root = repository.GetRootAsync poolId

            return
                match root |> Option.bind _.ActiveOperation with
                | Some owner when owner.OperationId = active.OperationId -> yieldUntil owner.LeaseExpiresAt active settled
                | _ -> report Yielded (Some active) settled
        }

    /// A write fenced by the pool root's conditions. Contention is tried again with backoff;
    /// a condition that no longer holds means ownership moved to another worker.
    let rec fenced (write: unit -> Async<GuardedWriteOutcome>) attempt =
        async {
            match! write () with
            | Written -> return true
            | ConditionNotMet -> return false
            | Contended when attempt + 1 < settings.GuardedWriteAttempts ->
                do! GuardedWriteRetry.backoff attempt
                return! fenced write (attempt + 1)
            | Contended -> return raise (InvalidOperationException "The pool root kept contending")
        }

    /// Persists the checkpoint under the current owner token. False means ownership was lost.
    let save (poolId: Ulid) (active: ActiveOperation) =
        fenced (fun () -> repository.ReplaceCheckpointAsync(poolId, active.OwnerToken, active)) 0

    let progressed (active: ActiveOperation) =
        { active with
            AttemptsWithoutProgress = 0
            LastProgressAt = now () }

    let release (poolId: Ulid) (active: ActiveOperation) =
        async {
            let! _ = save poolId { active with LeaseExpiresAt = now () }
            return report Yielded (Some active) 0
        }

    let republishDeferred (poolId: Ulid) =
        async {
            let! deferred = repository.GetDeferredReceiptsAsync poolId

            if not deferred.IsEmpty then
                do! commandPublisher.RepublishAsync(deferred |> List.map _.Command)
        }

    /// Completes the operation. A match settlement that opened a newer kickoff group makes its
    /// kickoff the pool's trend kickoff in the same fenced write.
    let complete (poolId: Ulid) (active: ActiveOperation) settled =
        async {
            let trendKickoff = PositionPolicy.completedTrendKickoff active.Target

            let! completed =
                fenced (fun () -> repository.CompleteAsync(poolId, active, active.UnresolvedDetails, trendKickoff)) 0

            if completed then
                do! republishDeferred poolId
                return report OperationCompleted (Some active) settled
            else
                return! yieldToOwner poolId active settled
        }

    let unresolvedCount (outcomes: DetailSettlementOutcome list) =
        outcomes
        |> List.filter (fun outcome ->
            match outcome with
            | PendingMaterialization
            | Held _ -> true
            | _ -> false)
        |> List.length

    let settledCount (outcomes: DetailSettlementOutcome list) =
        outcomes |> List.filter ((=) Settled) |> List.length

    /// Advances the active operation stage by stage until it completes, yields at the
    /// deadline, or loses ownership.
    let rec advance (poolId: Ulid) (deadline: DateTime) (active: ActiveOperation) settled =
        async {
            if now () >= deadline then
                return! release poolId active
            else
                match active.Stage, active.Target with
                | Scoring, MatchSettlement(matchId, result, _) -> return! scorePage poolId deadline active matchId result settled
                | Scoring, CurrentStandings -> return! moveToPositions poolId deadline active settled
                | Scoring, RejoinRestoration(gamblerId, generation) ->
                    return! restorePage poolId deadline active gamblerId generation settled
                | Positions, _ -> return! finalizePositions poolId deadline active settled
        }

    /// Saves the checkpoint and keeps advancing while this worker still owns the operation.
    and continueIfOwned poolId deadline (next: ActiveOperation) settled =
        async {
            let! owned = save poolId next
            return! (if owned then advance poolId deadline next settled else yieldToOwner poolId next settled)
        }

    and scorePage poolId deadline active matchId result settled =
        async {
            let! page, next = repository.GetMembershipPageAsync(poolId, active.Cursor, settings.MembershipPageSize)
            let! outcomes = SettlementPass.run repository settings poolId active matchId result now page
            let settled = settled + settledCount outcomes

            let active =
                { progressed active with
                    UnresolvedDetails = active.UnresolvedDetails + unresolvedCount outcomes }

            match next with
            | Some cursor -> return! continueIfOwned poolId deadline { active with Cursor = Some cursor } settled
            | None -> return! endScoringPass poolId deadline active settled
        }

    /// Restores one page of the gambler's retained details. The pass ends when the details
    /// are exhausted or the membership ended; current standings are then recalculated only if
    /// a detail was rebound, since the join's own standings command already placed the
    /// membership.
    and restorePage poolId deadline active gamblerId generation settled =
        async {
            let! page = RestorePass.run repository layoutMatchReader settings poolId active gamblerId generation
            let settled = settled + page.Restored

            let active =
                { progressed active with
                    UnresolvedDetails = active.UnresolvedDetails + page.Held
                    RestoredDetails = active.RestoredDetails + page.Restored }

            match page.Next with
            | Some cursor -> return! continueIfOwned poolId deadline { active with Cursor = Some cursor } settled
            | None when active.RestoredDetails > 0 -> return! moveToPositions poolId deadline active settled
            | None -> return! complete poolId active settled
        }

    /// A scoring pass ends only if the membership set did not change during it; otherwise
    /// the idempotent pass runs again over the current memberships.
    and endScoringPass poolId deadline active settled =
        async {
            let! root = repository.GetRootAsync poolId
            let revision = root |> Option.map _.MembershipRevision |> Option.defaultValue active.MembershipRevision

            if revision <> active.MembershipRevision then
                let restarted =
                    { active with
                        Cursor = None
                        UnresolvedDetails = 0
                        MembershipRevision = revision }

                return! continueIfOwned poolId deadline restarted settled
            else
                return! moveToPositions poolId deadline active settled
        }

    and moveToPositions poolId deadline active settled =
        async {
            let! root = repository.GetRootAsync poolId

            let next =
                { progressed active with
                    Stage = Positions
                    Cursor = None
                    CalculationRevision = active.CalculationRevision + 1
                    MembershipRevision = root |> Option.map _.MembershipRevision |> Option.defaultValue active.MembershipRevision }

            return! continueIfOwned poolId deadline next settled
        }

    and finalizePositions poolId deadline active settled =
        async {
            match! PositionsPass.run repository settings poolId active deadline now with
            | PositionsInterrupted -> return! release poolId active
            | PositionsInvalidated -> return! recalculate poolId deadline active settled
            | PositionsApplied unresolved ->
                let! root = repository.GetRootAsync poolId

                match root with
                | Some root when root.MembershipRevision = active.MembershipRevision ->
                    return! complete poolId { active with UnresolvedDetails = active.UnresolvedDetails + unresolved } settled
                | _ -> return! recalculate poolId deadline active settled
        }

    and recalculate poolId deadline active settled =
        async {
            let! root = repository.GetRootAsync poolId

            let next =
                { active with
                    CalculationRevision = active.CalculationRevision + 1
                    MembershipRevision = root |> Option.map _.MembershipRevision |> Option.defaultValue active.MembershipRevision }

            return! continueIfOwned poolId deadline next settled
        }

    /// Takes ownership of a checkpoint left by an earlier delivery. Repeated claims without
    /// progress mark the operation blocked instead of retrying forever.
    let claim (poolId: Ulid) (deadline: DateTime) (active: ActiveOperation) =
        async {
            let attempts = active.AttemptsWithoutProgress + 1

            if active.Blocked then
                return report Blocked (Some active) 0
            elif active.LeaseExpiresAt > now () then
                // Another worker holds the lease: wait for it to end rather than spin.
                return yieldUntil active.LeaseExpiresAt active 0
            elif attempts > settings.AttemptsBeforeBlocked then
                let! _ = save poolId { active with Blocked = true; AttemptsWithoutProgress = attempts }
                return report Blocked (Some active) 0
            else
                let claimed =
                    { active with
                        OwnerToken = newToken ()
                        LeaseExpiresAt = now () + settings.LeaseDuration
                        AttemptsWithoutProgress = attempts }

                let! owned = fenced (fun () -> repository.ReplaceCheckpointAsync(poolId, active.OwnerToken, claimed)) 0
                return! (if owned then advance poolId deadline claimed 0 else yieldToOwner poolId active 0)
        }

    /// Resolves the command to its immutable target. A match settlement compares its match's
    /// kickoff with the root's trend kickoff here, once, so every resume of the operation
    /// applies the same baseline decision.
    let resolveTarget (root: PoolSettlementRoot) (command: PoolCommand) =
        let settlement matchId result (state: PoolLayoutMatchSnapshot option) =
            let kickoff = state |> Option.map _.MatchDateTime
            MatchSettlement(matchId, result, PositionPolicy.trendEffect root.TrendKickoffDateTime kickoff)

        async {
            match command.Kind with
            | SettleResult(matchId, result) ->
                let! state = layoutMatchReader.GetMatchAsync(root.PoolLayoutId, matchId)
                return Some(settlement matchId result state)
            | ReconcileStandings -> return Some CurrentStandings
            | RestoreRejoinHistory(gamblerId, generation) -> return Some(RejoinRestoration(gamblerId, generation))
            | ReconcileMatch matchId ->
                let! state = layoutMatchReader.GetMatchAsync(root.PoolLayoutId, matchId)

                return
                    state
                    |> Option.bind _.Result
                    |> Option.map (fun result -> settlement matchId result state)
        }

    let receiptOf (command: PoolCommand) state revision =
        { OperationId = command.OperationId
          Revision = revision
          Fingerprint = PoolCommand.fingerprint command
          State = state
          Command = command
          DeferredAt = None }

    let newCheckpoint (root: PoolSettlementRoot) (command: PoolCommand) target =
        let admittedAt = now ()

        { OperationId = command.OperationId
          Revision = root.OperationRevision + 1L
          Target = target
          Stage =
            match target with
            | MatchSettlement _
            | RejoinRestoration _ -> Scoring
            | CurrentStandings -> Positions
          Cursor = None
          CalculationRevision = 1
          MembershipRevision = root.MembershipRevision
          OwnerToken = newToken ()
          LeaseExpiresAt = admittedAt + settings.LeaseDuration
          AttemptsWithoutProgress = 0
          Blocked = false
          UnresolvedDetails = 0
          RestoredDetails = 0
          AdmittedAt = admittedAt
          LastProgressAt = admittedAt }

    let rec handle (command: PoolCommand) (deadline: DateTime) =
        async {
            let! root = repository.GetRootAsync command.PoolId

            match root with
            | None -> return report PoolNotFound None 0
            | Some root ->
                match root.ActiveOperation with
                | Some active when active.OperationId = command.OperationId -> return! claim root.PoolId deadline active
                | Some active when active.Blocked -> return! defer command
                | Some active -> return! assist command deadline active
                | None -> return! admit root command deadline
        }

    and assist command deadline active =
        async {
            let! assisted = claim command.PoolId deadline active

            match assisted.Outcome with
            | OperationCompleted -> return! handle command deadline
            | Blocked -> return! defer command
            | _ -> return assisted
        }

    and defer command =
        async {
            do! repository.RecordDeferredAsync(command.PoolId, { receiptOf command Deferred None with DeferredAt = Some(now ()) })
            return report DeferredWhileBlocked None 0
        }

    and admit root command deadline =
        async {
            let! receipt = repository.GetReceiptAsync(root.PoolId, command.OperationId)

            match receipt with
            | Some existing when existing.Fingerprint <> PoolCommand.fingerprint command ->
                return
                    raise (
                        InvalidOperationException
                            $"Operation {command.OperationId} was reused with a different payload: {existing.Fingerprint}"
                    )
            | Some { State = Completed } ->
                do! republishDeferred root.PoolId
                return report AlreadyCompleted None 0
            | Some { State = Admitted } ->
                return
                    raise (
                        InvalidOperationException
                            $"Operation {command.OperationId} is admitted but not active on pool {root.PoolId.Value}"
                    )
            | Some { State = Deferred }
            | None -> return! start root command deadline
        }

    and start root command deadline =
        async {
            match! resolveTarget root command with
            | None ->
                do! repository.RecordCompletedAsync(root.PoolId, receiptOf command Completed None)
                return report OperationCompleted None 0
            | Some target ->
                let active = newCheckpoint root command target
                let receipt = receiptOf command Admitted (Some active.Revision)
                let! admitted = fenced (fun () -> repository.AdmitAsync(root, receipt, active)) 0

                if admitted then
                    return! advance root.PoolId deadline active 0
                else
                    return! handle command deadline
        }

    /// Processes the command until it completes or must be delivered again. The deadline is
    /// when the invocation must stop starting new work.
    member this.ExecuteAsync(command: PoolCommand, deadline: DateTime) : Async<PoolCommandReport> =
        handle command deadline
