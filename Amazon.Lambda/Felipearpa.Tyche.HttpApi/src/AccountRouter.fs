namespace Felipearpa.Tyche.HttpApi

open System
open System.Security.Claims
open Felipearpa.Tyche.Account.Application
open Felipearpa.Tyche.Account.Domain
open Felipearpa.Tyche.Function.AccountFunction
open Felipearpa.Tyche.Function.Request
open Felipearpa.Tyche.Pool.Application
open Microsoft.AspNetCore.Builder
open Microsoft.AspNetCore.Http

[<AutoOpen>]
module AccountRouter =

    type WebApplication with

        member this.ConfigureAccountRoutes() =
            this
                .MapPost(
                    "/accounts",
                    Func<_, _, _>(fun (linkAccountRequest: LinkAccountRequest) (linkAccount: LinkAccount) ->
                        async { return! linkAccountAsync linkAccountRequest linkAccount }
                        |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
                .MapPatch(
                    "/accounts",
                    Func<_, _, _>(fun (updateUsernameRequest: UpdateUsernameRequest) (updateUsername: UpdateUsername) ->
                        async { return! updateUsernameAsync updateUsernameRequest updateUsername }
                        |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
                .MapPost(
                    "/accounts/{accountId}/avatar-upload-url",
                    Func<_, _, _, _, _, _>
                        (fun
                            (accountId: string)
                            (avatarUploadUrlRequest: AvatarUploadUrlRequest)
                            (user: ClaimsPrincipal)
                            (accountRepository: IAccountRepository)
                            (issueAvatarUploadUrl: IssueAvatarUploadUrl) ->
                            async {
                                let! callerResult =
                                    CallerResolver.resolveCallerGamblerIdAsync user accountRepository

                                match callerResult with
                                | Error _ -> return Results.Unauthorized()
                                | Ok callerAccountId ->
                                    return!
                                        issueAvatarUploadUrlAsync
                                            accountId
                                            callerAccountId
                                            avatarUploadUrlRequest
                                            issueAvatarUploadUrl
                            }
                            |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
