namespace Felipearpa.User.Api

open System
open System.Threading.Tasks
open Felipearpa.Core
open Felipearpa.Core.Jwt
open Felipearpa.User.Api.Request.UserCreation
open Felipearpa.User.Api.Request.Login
open Felipearpa.User.Application.UserCreation
open Felipearpa.User.Application.Login
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
            this.MapPost(
                "/user",
                Func<_, _, _, _>
                    (fun
                        (createUserRequest: CreateUserRequest)
                        (jwtGenerator: IJwtGenerator)
                        (createUserCommandHandler: CreateUserCommandHandler) ->
                        async {
                            let! result =
                                createUserCommandHandler.ExecuteAsync(
                                    createUserRequest |> CreateUserMapper.mapToCommand
                                )

                            return
                                match result with
                                | Ok userViewModel ->
                                    Results.Ok(
                                        {| User = userViewModel
                                           Token =
                                            jwtGenerator.GenerateToken(
                                                { JwtSubject.NameIdentifier = userViewModel.UserId
                                                  Name = userViewModel.Username }
                                            ) |}
                                    )
                                | Error failure -> Results.Conflict(failure)
                        }
                        |> Async.StartAsTask)
            )
            |> ignore

            this.MapPost(
                "/user/login",
                Func<_, _, _, _>
                    (fun
                        (loginRequest: LoginRequest)
                        (jwtGenerator: IJwtGenerator)
                        (loginCommandHandler: LoginCommandHandler) ->
                        async {
                            let! result = loginCommandHandler.ExecuteAsync(loginRequest |> LoginMapper.mapToCommand)

                            return
                                match result with
                                | Ok userViewModel ->
                                    Results.Ok(
                                        {| User = userViewModel
                                           Token =
                                            jwtGenerator.GenerateToken(
                                                { JwtSubject.NameIdentifier = userViewModel.UserId
                                                  Name = userViewModel.Username }
                                            ) |}
                                    )
                                | Error _ -> Results.Unauthorized()
                        }
                        |> Async.StartAsTask)
            )
            |> ignore

            this
