namespace Felipearpa.Tyche.Function

open Felipearpa.Tyche.Account.Application.SignIn
open Felipearpa.Tyche.Function.Request
open Felipearpa.Tyche.Function.Request.LinkAccountRequestTransformer
open Microsoft.AspNetCore.Http

module AccountFunction =

    let linkAccount (linkAccountRequest: LinkAccountRequest) (linkAccountCommand: LinkAccountCommand) : IResult Async =
        async {
            let! result = linkAccountCommand.ExecuteAsync(linkAccountRequest.ToLinkAccountCommandInput())

            return
                match result with
                | Ok accountViewModel -> Results.Ok(accountViewModel)
                | Error _ -> Results.Unauthorized()
        }
