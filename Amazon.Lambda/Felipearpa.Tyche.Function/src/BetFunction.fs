namespace Felipearpa.Tyche.Function

open Felipearpa.Tyche.Function.Request
open Felipearpa.Tyche.Function.Response
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type
open Microsoft.AspNetCore.Http

module BetFunction =

    let bet (betRequest: BetRequest) (betCommand: BetCommand) : IResult Async =
        async {
            let betScore =
                { TeamScore.HomeTeamValue = betRequest.HomeTeamBet |> BetScore.newOf
                  AwayTeamValue = betRequest.AwayTeamBet |> BetScore.newOf }

            let! result =
                betCommand.ExecuteAsync(
                    betRequest.PoolId |> Ulid.newOf,
                    betRequest.GamblerId |> Ulid.newOf,
                    betRequest.MatchId |> Ulid.newOf,
                    betScore
                )

            return
                match result with
                | Ok bet -> Results.Ok(bet |> PoolGamblerBetTransformer.toResponse)
                | Error _ -> Results.Forbid()
        }
