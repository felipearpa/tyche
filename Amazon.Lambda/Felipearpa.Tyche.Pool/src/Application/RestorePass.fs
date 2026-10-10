namespace Felipearpa.Tyche.Pool.Application

open System
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

/// The verified outcome of restoring one retained detail.
type DetailRestoreOutcome =
    /// Rebound to the membership, now or by an earlier attempt whose response was lost.
    | Restored
    /// Held for repair with the exclusion marker; not counted in the total.
    | HeldForRepair
    /// Not a retained computed detail.
    | Skipped
    /// The membership was removed or replaced; nothing more may be added to it.
    | MembershipGone

type RestorePageResult =
    /// Details of the page this operation rebound, including any an interrupted earlier
    /// attempt rebound before its checkpoint was saved.
    { Restored: int
      Held: int
      /// The last match ID of the page when more details follow; None when the gambler's
      /// details are exhausted or the membership ended.
      Next: string option }

/// Rebinds a page of a gambler's retained details to the rejoined membership. Every write
/// checks the same membership total, so the details are restored one at a time and the
/// membership state each write produced is carried to the next.
module RestorePass =

    type private RestoreContext =
        { Repository: IPoolSettlementRepository
          Settings: PoolOperationSettings
          PoolId: Ulid
          GamblerId: Ulid
          Generation: string
          Active: ActiveOperation }

    let private matchStateOf (snapshot: PoolLayoutMatchSnapshot option) =
        match snapshot with
        | None -> MatchMissing
        | Some { Result = None } -> MatchUnscored
        | Some { Result = Some result } -> MatchScored result

    let private belongsToTarget (context: RestoreContext) (membership: MembershipState option) =
        membership |> Option.exists (fun current -> current.Generation = Some context.Generation)

    let private afterRebinding (change: RebindChange) (membership: MembershipState option) =
        membership
        |> Option.map (fun current ->
            { current with
                Total = Some change.Total
                ScoreVersion = Some change.ScoreVersion })

    let rec private restoreDetail
        (context: RestoreContext)
        (matchState: RetainedMatch)
        (attempt: int)
        (membership: MembershipState option)
        (bet: RetainedBetState)
        : Async<DetailRestoreOutcome * MembershipState option> =
        async {
            match RestorePolicy.decide context.Generation membership bet matchState with
            | MembershipEnded -> return MembershipGone, membership
            | AlreadyRestored -> return Restored, membership
            | AlreadyHeld -> return HeldForRepair, membership
            | NotRetained -> return Skipped, membership
            | InvalidMembership reason ->
                return
                    raise (
                        InvalidOperationException
                            $"Restoration {context.Active.OperationId} cannot use membership {context.GamblerId.Value}: {reason}"
                    )
            | HoldForRepair reason ->
                let hold =
                    { RestoreHold.PoolId = context.PoolId
                      GamblerId = context.GamblerId
                      MatchId = bet.Settlement.MatchId
                      ReadGeneration = bet.Settlement.Generation
                      Generation = context.Generation
                      Reason = reason }

                match! context.Repository.HoldRetainedBetAsync hold with
                | Written -> return HeldForRepair, membership
                | failed -> return! retry context matchState attempt failed bet
            | Rebind change ->
                let write =
                    { RebindWrite.PoolId = context.PoolId
                      GamblerId = context.GamblerId
                      MatchId = bet.Settlement.MatchId
                      OperationId = context.Active.OperationId
                      OperationRevision = context.Active.Revision
                      Change = change }

                match! context.Repository.ApplyRebindingAsync write with
                | Written -> return Restored, afterRebinding change membership
                | failed -> return! retry context matchState attempt failed bet
        }

    /// Re-reads both records after a failed condition, so the outcome is classified from
    /// persisted state: a lost response finds the detail already restored, a changed score
    /// version or detail is decided again, and a removed membership ends the pass.
    and private retry context matchState attempt (failed: GuardedWriteOutcome) (bet: RetainedBetState) =
        async {
            if attempt + 1 >= context.Settings.GuardedWriteAttempts then
                return
                    raise (
                        InvalidOperationException
                            $"Restoration of {bet.Settlement.MatchId.Value} for {context.Active.OperationId} did not converge: {failed}"
                    )
            else
                if failed = Contended then
                    do! GuardedWriteRetry.backoff attempt

                let! membership = context.Repository.GetMembershipAsync(context.PoolId, context.GamblerId)
                let! current = context.Repository.GetRetainedBetAsync(context.PoolId, context.GamblerId, bet.Settlement.MatchId)

                match current with
                | Some current -> return! restoreDetail context matchState (attempt + 1) membership current
                | None when belongsToTarget context membership -> return Skipped, membership
                | None -> return MembershipGone, membership
        }

    /// Restores the retained details of one page after the active operation's cursor.
    let run
        (repository: IPoolSettlementRepository)
        (layoutMatchReader: IPoolLayoutMatchReader)
        (settings: PoolOperationSettings)
        (poolId: Ulid)
        (active: ActiveOperation)
        (gamblerId: Ulid)
        (generation: string)
        : Async<RestorePageResult> =
        let context =
            { Repository = repository
              Settings = settings
              PoolId = poolId
              GamblerId = gamblerId
              Generation = generation
              Active = active }

        let ended restored held =
            { Restored = restored
              Held = held
              Next = None }

        let rec loop (membership: MembershipState) (remaining: RetainedBetState list) restored held next =
            async {
                match remaining with
                | [] ->
                    return
                        { Restored = restored
                          Held = held
                          Next = next }
                | bet :: rest ->
                    let! snapshot = layoutMatchReader.GetMatchAsync(membership.PoolLayoutId, bet.Settlement.MatchId)
                    let! outcome, current = restoreDetail context (matchStateOf snapshot) 0 (Some membership) bet

                    match outcome, current with
                    | Restored, Some current -> return! loop current rest (restored + 1) held next
                    | HeldForRepair, Some current -> return! loop current rest restored (held + 1) next
                    | Skipped, Some current -> return! loop current rest restored held next
                    | _ -> return ended restored held
            }

        async {
            let! membership = repository.GetMembershipAsync(poolId, gamblerId)

            match membership with
            | Some membership when membership.Generation = Some generation ->
                let! page, next = repository.GetRetainedBetPageAsync(poolId, gamblerId, active.Cursor, settings.MembershipPageSize)

                let reboundEarlier =
                    page
                    |> List.filter (fun bet ->
                        bet.Settlement.Generation = Some generation
                        && bet.SettlementOperationId = Some active.OperationId)
                    |> List.length

                return! loop membership (page |> List.filter (RestorePolicy.isRetained generation)) reboundEarlier 0 next
            | _ -> return ended 0 0
        }
