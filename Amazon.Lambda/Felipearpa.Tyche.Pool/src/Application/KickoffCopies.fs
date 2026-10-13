namespace Felipearpa.Tyche.Pool.Application

open System
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

/// Applies a kickoff-copy plan to match details discovered through the match index. Each
/// write is guarded by the copies it was planned from; a failed guard re-reads the detail
/// strongly and plans again, so a stale index entry or a concurrent settlement never writes a
/// wrong value and a converged detail is never written.
module internal KickoffCopies =

    [<Literal>]
    let private attempts = 5

    [<Literal>]
    let private concurrency = 16

    let rec private apply
        (repository: IDetailKickoffRepository)
        (plan: DetailKickoff -> KickoffCopyWrite option)
        (detail: DetailKickoff)
        (attempt: int)
        =
        async {
            match plan detail with
            | None -> return 0
            | Some write ->
                match! repository.RewriteKickoffAsync write with
                | Written -> return 1
                | ConditionNotMet
                | Contended when attempt + 1 < attempts ->
                    match! repository.GetDetailAsync(detail.PoolId, detail.GamblerId, detail.MatchId) with
                    | Some current -> return! apply repository plan current (attempt + 1)
                    | None -> return 0
                | ConditionNotMet
                | Contended ->
                    return
                        raise (
                            InvalidOperationException
                                $"The kickoff copies of match {detail.MatchId.Value} for gambler {detail.GamblerId.Value} in pool {detail.PoolId.Value} did not converge"
                        )
        }

    /// Applies the plan to every detail of the match, in every pool or only in the given one,
    /// and returns how many details were written.
    let applyToMatch
        (repository: IDetailKickoffRepository)
        (plan: DetailKickoff -> KickoffCopyWrite option)
        (matchId: Ulid)
        (poolId: Ulid option)
        =
        let rec loop (next: string option) (written: int) =
            async {
                let! details, next = repository.GetMatchDetailsPageAsync(matchId, poolId, next)
                let! counts = Async.Parallel(details |> List.map (fun detail -> apply repository plan detail 0), concurrency)
                let written = written + Array.sum counts

                match next with
                | Some _ -> return! loop next written
                | None -> return written
            }

        loop None 0

    /// Whether the pool holds a computed detail of the match, as the match index sees it.
    let hasComputedDetail (repository: IDetailKickoffRepository) (matchId: Ulid) (poolId: Ulid) =
        let rec loop (next: string option) =
            async {
                let! details, next = repository.GetMatchDetailsPageAsync(matchId, Some poolId, next)

                match details |> List.exists _.IsComputed, next with
                | true, _ -> return true
                | false, Some _ -> return! loop next
                | false, None -> return false
            }

        loop None

/// Moves every gambler's copies of a rescheduled match's kickoff to the new instant, in every
/// pool using the layout, so the match leaves its old kickoff group in both feeds. The pool's
/// trend kickoff is not touched. Repeated deliveries of the change write nothing.
type PropagateKickoffChange(repository: IDetailKickoffRepository) =

    /// Returns the number of details written.
    member this.ExecuteAsync(matchId: Ulid, previousKickoff: DateTime, kickoff: DateTime) : Async<int> =
        KickoffCopies.applyToMatch repository (DetailKickoff.reschedule previousKickoff kickoff) matchId None

/// What one kickoff-group backfill job did.
type KickoffBackfillReport =
    { CopiedDetails: int
      /// The trend kickoff this job recorded on the pool root, if it recorded one.
      RecordedTrendKickoff: DateTime option }

/// Prepares an existing pool for the grouped feeds: copies the kickoff onto every computed
/// detail that lacks its finished-feed copy, a few layout matches per job through a durable
/// continuation, then records the pool's trend kickoff when the root has none: the kickoff of
/// its most recently completed result settlement or, for a pool settled before result receipts
/// existed, the latest kickoff among the layout matches with a computed detail in the pool. A
/// pool without computed details keeps no trend kickoff. Memberships keep their
/// `beforePosition`. Restarting is safe: converged details and a root that has a trend
/// kickoff are not written.
type BackfillPoolKickoffGroups
    (
        settlementRepository: IPoolSettlementRepository,
        detailKickoffRepository: IDetailKickoffRepository,
        layoutMatchReader: IPoolLayoutMatchReader,
        maintenancePublisher: IPoolMaintenancePublisher
    ) =

    [<Literal>]
    let matchesPerJob = 8

    [<Literal>]
    let everyVersion = Int32.MaxValue

    [<Literal>]
    let trendAttempts = 3

    let rec layoutMatches (poolLayoutId: Ulid) (next: string option) (acc: PoolLayoutMatchSnapshot list) =
        async {
            let! page = layoutMatchReader.GetMatchesThroughVersionAsync(poolLayoutId, everyVersion, next)
            let acc = acc @ List.ofSeq page.Items

            match page.Next with
            | Some _ -> return! layoutMatches poolLayoutId page.Next acc
            | None -> return acc
        }

    /// The match of the completed result settlement with the highest operation revision;
    /// operations of a pool run one at a time, so it is the most recently completed one.
    let latestSettledMatch (receipts: OperationReceipt list) =
        receipts
        |> List.choose (fun receipt ->
            match receipt.State, receipt.Command.Kind with
            | Completed, SettleResult(matchId, _) -> Some(receipt.Revision |> Option.defaultValue 0L, matchId)
            | _ -> None)
        |> List.sortBy fst
        |> List.tryLast
        |> Option.map snd

    /// The latest kickoff among the layout matches that have a computed detail in the pool,
    /// checked newest kickoff first, so a finished pool usually answers from its first query.
    /// Under the per-settlement rule that preceded result receipts, it is the kickoff whose
    /// settlement last reset `beforePosition`.
    let latestComputedKickoff (poolId: Ulid) (matches: PoolLayoutMatchSnapshot list) =
        let rec firstComputed (groups: (DateTime * Ulid list) list) =
            async {
                match groups with
                | [] -> return None
                | (kickoff, matchIds) :: older ->
                    let! computed =
                        matchIds
                        |> List.map (fun matchId -> KickoffCopies.hasComputedDetail detailKickoffRepository matchId poolId)
                        |> Async.Sequential

                    if Array.contains true computed then
                        return Some kickoff
                    else
                        return! firstComputed older
            }

        matches
        |> List.groupBy (fun layoutMatch -> KickoffGroup.instant layoutMatch.MatchDateTime)
        |> List.sortByDescending fst
        |> List.map (fun (kickoff, group) -> kickoff, group |> List.map _.MatchId)
        |> firstComputed

    /// The kickoff of the latest completed result settlement or, when the pool has no
    /// completed result receipt, the latest kickoff with a computed detail.
    let trendKickoffOf (poolId: Ulid) (matches: PoolLayoutMatchSnapshot list) =
        async {
            let! receipts = settlementRepository.GetCompletedResultReceiptsAsync poolId

            match latestSettledMatch receipts with
            | None -> return! latestComputedKickoff poolId matches
            | Some matchId ->
                match matches |> List.tryFind (fun layoutMatch -> layoutMatch.MatchId = matchId) with
                | Some layoutMatch -> return Some layoutMatch.MatchDateTime
                | None ->
                    return
                        raise (
                            InvalidOperationException
                                $"Match {matchId.Value} of pool {poolId.Value}'s latest settlement is not in its layout"
                        )
        }

    /// Records the trend kickoff when the root has none and no operation is active. An active
    /// operation fails the job so the queue retries it after a visibility timeout: the
    /// operation either records a trend kickoff itself or leaves the pool idle for the retry.
    let rec recordTrendKickoff (poolId: Ulid) (matches: PoolLayoutMatchSnapshot list) (attempt: int) =
        async {
            match! settlementRepository.GetRootAsync poolId with
            | None -> return None
            | Some root when root.TrendKickoffDateTime.IsSome -> return None
            | Some root when root.ActiveOperation.IsSome ->
                return
                    raise (
                        InvalidOperationException
                            $"Pool {poolId.Value} has an active operation; its trend kickoff is recorded on a later attempt"
                    )
            | Some root ->
                match! trendKickoffOf poolId matches with
                | None -> return None
                | Some kickoff ->
                    match! settlementRepository.RecordTrendKickoffAsync(root, kickoff) with
                    | Written -> return Some(KickoffGroup.instant kickoff)
                    | _ when attempt + 1 < trendAttempts -> return! recordTrendKickoff poolId matches (attempt + 1)
                    | _ ->
                        return
                            raise (
                                InvalidOperationException
                                    $"Pool {poolId.Value}'s root kept changing while its trend kickoff was recorded"
                            )
        }

    member this.ExecuteAsync(poolId: Ulid, afterMatchId: string option) : Async<KickoffBackfillReport> =
        async {
            match! settlementRepository.GetRootAsync poolId with
            | None ->
                return
                    { CopiedDetails = 0
                      RecordedTrendKickoff = None }
            | Some root ->
                let! matches = layoutMatches root.PoolLayoutId None []

                let remaining =
                    matches
                    |> List.sortBy _.MatchId.Value
                    |> List.filter (fun layoutMatch ->
                        afterMatchId |> Option.forall (fun cursor -> String.CompareOrdinal(layoutMatch.MatchId.Value, cursor) > 0))

                let batch = remaining |> List.truncate matchesPerJob

                let! copied =
                    batch
                    |> List.map (fun layoutMatch ->
                        KickoffCopies.applyToMatch
                            detailKickoffRepository
                            DetailKickoff.backfill
                            layoutMatch.MatchId
                            (Some poolId))
                    |> Async.Sequential

                if remaining.Length > batch.Length then
                    do! maintenancePublisher.PublishAsync [ BackfillKickoffGroups(poolId, Some (List.last batch).MatchId.Value) ]

                    return
                        { CopiedDetails = Array.sum copied
                          RecordedTrendKickoff = None }
                else
                    let! recorded = recordTrendKickoff poolId matches 0

                    return
                        { CopiedDetails = Array.sum copied
                          RecordedTrendKickoff = recorded }
        }
