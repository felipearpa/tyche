namespace Felipearpa.Tyche.Function

open System.Threading.Tasks
open Amazon.Lambda.Annotations.APIGateway
open Felipearpa.Core.Jwt
open Felipearpa.Tyche.Account.Application.SignIn
open Felipearpa.Tyche.Function.Request
open Felipearpa.Tyche.Function.Request.SignInRequestTransformer

type AccountFunction() =

    member this.LinkAccount
        (
            linkAccountRequest: LinkAccountRequest,
            jwtGenerator: IJwtGenerator,
            loginCommandHandler: LoginCommandHandler
        ) : Task<IHttpResult> =
        async {
            let! result = loginCommandHandler.ExecuteAsync(linkAccountRequest.ToLinkAccountCommand())

            return
                match result with
                | Ok accountViewModel -> HttpResults.Ok(accountViewModel)
                | Error _ -> HttpResults.Unauthorized()
        }
        |> Async.StartAsTask
