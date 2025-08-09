namespace Felipearpa.Tyche.Function

open Felipearpa.Tyche.Account.Application
open Felipearpa.Tyche.Function.Request
open Felipearpa.Tyche.Function.Request.LinkAccountRequestTransformer
open Felipearpa.Tyche.Function.Response.AccountTransformer
open Microsoft.AspNetCore.Http

module AccountFunction =

    let linkAccountAsync
        (linkAccountRequest: LinkAccountRequest)
        (linkAccountCommand: LinkAccountCommand)
        : IResult Async =
        async {
            let! result = linkAccountCommand.ExecuteAsync(linkAccountRequest.ToLinkAccountCommandInput())

            return
                match result with
                | Ok account -> Results.Ok(account.ToAccountResponse())
                | Error _ -> Results.Unauthorized()
        }
