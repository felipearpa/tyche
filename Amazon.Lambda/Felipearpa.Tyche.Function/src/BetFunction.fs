namespace Felipearpa.Tyche.Function

open System.Threading.Tasks
open Amazon.Lambda.Annotations.APIGateway
open Felipearpa.Tyche.Function.Request
open Felipearpa.Tyche.Function.Response
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type

type BetFunction() =

    member this.PatchBet(betRequest: BetRequest, betCommand: BetCommand) : Task<IHttpResult> =
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
                | Ok bet -> HttpResults.Ok(bet |> PoolGamblerBetTransformer.toResponse)
                | Error _ -> HttpResults.Forbid()
        }
        |> Async.StartAsTask
