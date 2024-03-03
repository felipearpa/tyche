namespace Felipearpa.Account.Api

open System
open System.Threading.Tasks
open Felipearpa.Core
open Felipearpa.Core.Jwt
open Felipearpa.Account.Api.Request.SignIn
open Felipearpa.Account.Api.Request.SignIn.SignInRequestTransformer
open Felipearpa.Account.Application.SignIn
open Microsoft.AspNetCore.Builder
open Microsoft.AspNetCore.Http

[<AutoOpen>]
module WebApplication =

    type WebApplication with

        member this.ConfigureMiddleware() =
            this.UseExceptionHandler(fun builder ->
                builder.Run(fun context ->
                    ExceptionWriter.asyncCreateResponseForError context (ExceptionTransformer())
                    |> Async.StartAsTask
                    :> Task))
            |> ignore

            this

        member this.ConfigureRoutes() =
            this
                .MapPost(
                    "/account/link",
                    Func<_, _, _, _>
                        (fun
                            (linkAccountRequest: LinkAccountRequest)
                            (jwtGenerator: IJwtGenerator)
                            (loginCommandHandler: LoginCommandHandler) ->
                            async {
                                let! result =
                                    loginCommandHandler.ExecuteAsync(linkAccountRequest.ToLinkAccountCommand())

                                return
                                    match result with
                                    | Ok accountViewModel -> Results.Ok(accountViewModel)
                                    | Error _ -> Results.Unauthorized()
                            }
                            |> Async.StartAsTask)
                )
                // .RequireAuthorization()
            |> ignore

            this
