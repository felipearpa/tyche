namespace Felipearpa.Tyche.Function

open Felipearpa.Core.Paging
open Felipearpa.Tyche.Function.Request
open Felipearpa.Tyche.Function.Response
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type
open Microsoft.AspNetCore.Http

module PoolFunction =

    let getPoolByIdAsync (poolId: string) (getPoolById: GetPoolById) : IResult Async =
        async {
            let! result = getPoolById.ExecuteAsync(poolId |> Ulid.newOf)

            return
                match result with
                | Ok maybePool ->
                    match maybePool with
                    | None -> Results.NotFound("Pool not found")
                    | Some pool -> Results.Ok(pool |> PoolTransformer.toPoolViewModel)
                | Error _ -> Results.InternalServerError()
        }

    let getGamblersByPoolIdAsync
        (poolId: string)
        (next: string option)
        (getPoolGamblerScoresByPool: GetPoolGamblerScoresByPool)
        : IResult Async =
        async {
            let! page = getPoolGamblerScoresByPool.ExecuteAsync(poolId |> Ulid.newOf, next)
            return Results.Ok(page |> CursorPage.map PoolGamblerScoreTransformer.ofStanding)
        }
        |> ContinuationResponse.orInvalidCursor

    let getPoolGamblerScoreByIdAsync
        (poolId: string)
        (gamblerId: string)
        (getPoolGamblerScoreById: GetPoolGamblerScoreById)
        : IResult Async =
        async {
            let! result = getPoolGamblerScoreById.ExecuteAsync(poolId |> Ulid.newOf, gamblerId |> Ulid.newOf)

            return
                match result with
                | Ok maybeScore ->
                    match maybeScore with
                    | None -> Results.NotFound()
                    | Some standing -> Results.Ok(standing |> PoolGamblerScoreTransformer.ofStanding)
                | Error _ -> Results.InternalServerError()
        }

    let getPendingBetsAsync
        (poolId: string)
        (gamblerId: string)
        (searchText: string option)
        (next: string option)
        (getPendingPoolGamblerBets: GetPendingPoolGamblerBets)
        : IResult Async =
        async {
            let! page =
                getPendingPoolGamblerBets.ExecuteAsync(poolId |> Ulid.newOf, gamblerId |> Ulid.newOf, searchText, next)

            return Results.Ok(page |> CursorPage.map PoolGamblerBetTransformer.toResponse)
        }
        |> ContinuationResponse.orInvalidCursor

    /// History: whole kickoff groups, newest kickoff first.
    let getFinishedBetsAsync
        (poolId: string)
        (gamblerId: string)
        (next: string option)
        (getFinishedPoolGamblerBets: GetFinishedPoolGamblerBets)
        : IResult Async =
        async {
            let! page = getFinishedPoolGamblerBets.ExecuteAsync(poolId |> Ulid.newOf, gamblerId |> Ulid.newOf, next)
            return Results.Ok(page |> CursorPage.map KickoffGroupTransformer.toResponse)
        }
        |> ContinuationResponse.orInvalidCursor

    let getLiveBetsAsync
        (poolId: string)
        (gamblerId: string)
        (searchText: string option)
        (next: string option)
        (getLivePoolGamblerBets: GetLivePoolGamblerBets)
        : IResult Async =
        async {
            let! page =
                getLivePoolGamblerBets.ExecuteAsync(poolId |> Ulid.newOf, gamblerId |> Ulid.newOf, searchText, next)

            return Results.Ok(page |> CursorPage.map PoolGamblerBetTransformer.toResponse)
        }
        |> ContinuationResponse.orInvalidCursor

    /// Timeline: whole kickoff groups, unsettled groups first, then settled ones.
    let getBetsTimelineAsync
        (poolId: string)
        (gamblerId: string)
        (next: string option)
        (getGamblerBetsTimeline: GetGamblerBetsTimeline)
        : IResult Async =
        async {
            let! page = getGamblerBetsTimeline.ExecuteAsync(poolId |> Ulid.newOf, gamblerId |> Ulid.newOf, next)

            return Results.Ok(page |> CursorPage.map KickoffGroupTransformer.toResponse)
        }
        |> ContinuationResponse.orInvalidCursor

    let getMatchBetsAsync
        (poolId: string)
        (matchId: string)
        (next: string option)
        (getPoolMatchGamblerBets: GetPoolMatchGamblerBets)
        : IResult Async =
        async {
            let! page = getPoolMatchGamblerBets.ExecuteAsync(poolId |> Ulid.newOf, matchId |> Ulid.newOf, next)
            return Results.Ok(page |> CursorPage.map PoolGamblerBetTransformer.toResponse)
        }
        |> ContinuationResponse.orInvalidCursor

    /// The match-bets ordering mode that ranks participants by their pool totals.
    [<Literal>]
    let poolScoreDescendingSort = "pool-score-desc"

    let private rankedError (statusCode: int) (code: string) (message: string) =
        Results.Json({| error = code; message = message |}, statusCode = statusCode)

    let private rankedResponse (result: Result<PoolGamblerBet CursorPage, RankedMatchBetsFailure>) =
        match result with
        | Ok page -> Results.Ok(page |> CursorPage.map PoolGamblerBetTransformer.toResponse)
        | Error RankedMatchBetsFailure.PoolNotFound -> rankedError 404 "POOL_NOT_FOUND" "Pool not found"
        | Error RankedMatchBetsFailure.NotPoolMember ->
            rankedError 403 "NOT_POOL_MEMBER" "The requester is not a member of this pool"
        | Error RankedMatchBetsFailure.MatchNotFound -> rankedError 404 "MATCH_NOT_FOUND" "Match not found"
        | Error RankedMatchBetsFailure.PredictionsHidden ->
            rankedError 409 "PREDICTIONS_HIDDEN" "Predictions stay hidden until betting closes for this match"
        | Error RankedMatchBetsFailure.RankingNotReady ->
            rankedError 503 "RANKING_NOT_READY" "Ranked match bets are not available yet; try again later"
        | Error(RankedMatchBetsFailure.InvalidCursor CursorContextMismatch) ->
            rankedError
                400
                "CURSOR_CONTEXT_MISMATCH"
                "The continuation belongs to another pool, match, or order; restart from the first page"
        | Error(RankedMatchBetsFailure.InvalidCursor MalformedCursor) ->
            rankedError 400 "INVALID_CURSOR" "The continuation is not valid; restart from the first page"

    /// GET /pools/{poolId}/matches/{matchId}/bets?sort=pool-score-desc. Failures are explicit
    /// and recoverable; this mode never falls back to the legacy order.
    let getRankedMatchBetsAsync
        (requesterId: Ulid)
        (poolId: string)
        (matchId: string)
        (next: string option)
        (getRankedPoolMatchBets: GetRankedPoolMatchBets)
        : IResult Async =
        async {
            let! attempt =
                getRankedPoolMatchBets.ExecuteAsync(requesterId, Ulid.newOf poolId, Ulid.newOf matchId, next)
                |> Async.Catch

            match attempt with
            | Choice1Of2 result -> return rankedResponse result
            // An unreadable ranking index is a recoverable outage, never a reason to fall back
            // to another order.
            | Choice2Of2 error when (error.GetBaseException() :? Amazon.Runtime.AmazonServiceException) ->
                return rankedError 503 "RANKING_UNAVAILABLE" "Ranked match bets could not be read; try again later"
            | Choice2Of2 error -> return raise error
        }

    let getPoolGamblerBetByIdAsync
        (poolId: string)
        (gamblerId: string)
        (matchId: string)
        (getPoolGamblerBetById: GetPoolGamblerBetById)
        : IResult Async =
        async {
            let! maybeBet =
                getPoolGamblerBetById.ExecuteAsync(poolId |> Ulid.newOf, gamblerId |> Ulid.newOf, matchId |> Ulid.newOf)

            return
                match maybeBet with
                | Some bet -> Results.Ok(bet |> PoolGamblerBetTransformer.toResponse)
                | None -> Results.NotFound()
        }

    let createPoolAsync (createPoolRequest: CreatePoolRequest) (createPool: CreatePool) : IResult Async =
        async {
            let! result = createPool.ExecuteAsync(createPoolRequest |> CreatePoolRequestTransformer.toCreatePoolInput)

            return
                match result with
                | Ok pool -> Results.Ok(pool |> CreatePoolOutputTransformer.toResponse)
                | Error CreatePoolFailure.GamblerNotFound -> Results.NotFound("Gambler not found")
                | Error CreatePoolFailure.PoolLayoutNotFound -> Results.NotFound("Pool layout not found")
        }

    let joinPoolAsync (poolId: string) (joinPoolRequest: JoinPoolRequest) (joinPool: JoinPool) : IResult Async =
        async {
            let! result = joinPool.ExecuteAsync(joinPoolRequest |> JoinPoolRequestTransformer.toJoinPoolInput poolId)

            return
                match result with
                | Ok _ -> Results.NoContent()
                | Error error ->
                    match error with
                    | JoinPoolFailure.AlreadyJoined -> Results.Conflict("The gambler has already joined this pool")
                    | JoinPoolFailure.PoolNotFound -> Results.NotFound("Pool not found")
                    | JoinPoolFailure.GamblerNotFound -> Results.NotFound("Gambler not found")
                    | JoinPoolFailure.PoolLayoutNotFound -> Results.NotFound("Pool layout not found")
        }

    let deletePoolAsync (poolId: string) (gamblerId: string) (deletePool: DeletePool) : IResult Async =
        async {
            let! result =
                deletePool.ExecuteAsync(
                    { DeletePoolInput.PoolId = poolId |> Ulid.newOf
                      GamblerId = gamblerId |> Ulid.newOf }
                )

            return
                match result with
                | Ok _ -> Results.NoContent()
                | Error DeletePoolFailure.PoolNotFound -> Results.NotFound("Pool not found")
                | Error DeletePoolFailure.NotOwner -> Results.Forbid()
        }
