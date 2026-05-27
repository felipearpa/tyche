namespace Felipearpa.Tyche.HttpApi

open System
open System.Security.Claims
open Felipearpa.Tyche.Account.Domain
open Felipearpa.Tyche.Function.GamblerFunction
open Felipearpa.Tyche.Pool.Application
open Microsoft.AspNetCore.Builder
open Microsoft.AspNetCore.Http

[<AutoOpen>]
module GamblerRouter =

    type WebApplication with

        member this.ConfigureGamblerRoutes() =
            this
                .MapGet(
                    "/gamblers/{gamblerId}/pools",
                    Func<_, _, _, _>
                        (fun
                            (gamblerId: string)
                            (next: string)
                            (getPoolGamblerScoresByGambler: GetPoolGamblerScoresByGambler) ->
                            async {
                                return!
                                    getPoolsByGamblerIdAsync
                                        gamblerId
                                        (next |> Option.ofObj)
                                        getPoolGamblerScoresByGambler
                            }
                            |> Async.StartAsTask)

                )
                .RequireAuthorization()
            |> ignore

            this
                .MapGet(
                    "/pools/{poolId}/members",
                    Func<_, _, _, _, _, _>
                        (fun
                            (poolId: string)
                            (next: string)
                            (user: ClaimsPrincipal)
                            (accountRepository: IAccountRepository)
                            (getPoolMembers: GetPoolMembers) ->
                            async {
                                let! callerResult =
                                    CallerResolver.resolveCallerGamblerIdAsync user accountRepository

                                match callerResult with
                                | Error _ -> return Results.Unauthorized()
                                | Ok callerGamblerId ->
                                    return!
                                        getPoolMembersAsync poolId callerGamblerId (next |> Option.ofObj) getPoolMembers
                            }
                            |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
                .MapDelete(
                    "/pools/{poolId}/members/{gamblerId}",
                    Func<_, _, _, _, _, _>
                        (fun
                            (poolId: string)
                            (gamblerId: string)
                            (user: ClaimsPrincipal)
                            (accountRepository: IAccountRepository)
                            (removeGambler: RemovePoolGambler) ->
                            async {
                                let! callerResult =
                                    CallerResolver.resolveCallerGamblerIdAsync user accountRepository

                                match callerResult with
                                | Error _ -> return Results.Unauthorized()
                                | Ok callerGamblerId ->
                                    return! removeGamblerAsync poolId gamblerId callerGamblerId removeGambler
                            }
                            |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
