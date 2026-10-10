namespace Felipearpa.Tyche.Pool.Application

open System
open Felipearpa.Core.Paging
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type RankedMatchBetsFailure =
    | PoolNotFound
    /// The requester has no current membership, including while their removal cleanup lags.
    | NotPoolMember
    | MatchNotFound
    /// Predictions are still open under the existing lock policy.
    | PredictionsHidden
    /// The pool's projection has not been verified ready.
    | RankingNotReady
    | InvalidCursor of RankedCursorError

type RankedMatchBetsSettings =
    { PageSize: int }

    static member Default = { PageSize = 50 }

/// Returns one page of a match's participants ordered by their propagated pool totals.
/// Requester membership, the reveal gate, and readiness come from authoritative reads on
/// every request, independently of the projection rows and of any cursor.
type GetRankedPoolMatchBets
    (
        settlementRepository: IPoolSettlementRepository,
        projectionRepository: IPoolProjectionRepository,
        layoutMatchReader: IPoolLayoutMatchReader,
        rankedRepository: IRankedMatchBetsRepository,
        settings: RankedMatchBetsSettings,
        now: unit -> DateTime
    ) =

    let isRevealed (layoutMatch: PoolLayoutMatchSnapshot) =
        layoutMatch.Result.IsSome || LockPolicy.isLockedAt (now ()) layoutMatch.MatchDateTime

    let page poolId matchId cursor =
        async {
            let! result = rankedRepository.GetPageAsync(poolId, matchId, cursor, settings.PageSize)
            return result |> Result.mapError InvalidCursor
        }

    let afterAccess (root: PoolSettlementRoot) matchId cursor =
        async {
            match! layoutMatchReader.GetMatchAsync(root.PoolLayoutId, matchId) with
            | None -> return Error MatchNotFound
            | Some layoutMatch when not (isRevealed layoutMatch) -> return Error PredictionsHidden
            | Some _ when root.RankingReadiness <> Some Ready -> return Error RankingNotReady
            | Some _ -> return! page root.PoolId matchId cursor
        }

    member this.ExecuteAsync
        (requesterId: Ulid, poolId: Ulid, matchId: Ulid, cursor: string option)
        : Async<Result<PoolGamblerBet CursorPage, RankedMatchBetsFailure>> =
        async {
            match! settlementRepository.GetRootAsync poolId with
            | None -> return Error PoolNotFound
            | Some root ->
                match! projectionRepository.GetMembershipAsync(poolId, requesterId) with
                | None -> return Error NotPoolMember
                | Some _ -> return! afterAccess root matchId cursor
        }
