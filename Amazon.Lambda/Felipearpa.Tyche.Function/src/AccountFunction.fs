namespace Felipearpa.Tyche.Function

open Felipearpa.Tyche.Account.Application
open Felipearpa.Tyche.Function.Request
open Felipearpa.Tyche.Function.Request.LinkAccountRequestTransformer
open Felipearpa.Tyche.Function.Response.AccountTransformer
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Type
open Microsoft.AspNetCore.Http

module AccountFunction =

    let linkAccountAsync (linkAccountRequest: LinkAccountRequest) (linkAccount: LinkAccount) : IResult Async =
        async {
            let! result = linkAccount.ExecuteAsync(linkAccountRequest.ToLinkAccountInput())

            return
                match result with
                | Ok account -> Results.Ok(account.ToAccountResponse())
                | Error _ -> Results.InternalServerError()
        }

    let updateUsernameAsync (request: UpdateUsernameRequest) (updateUsername: UpdateUsername) : IResult Async =
        async {
            let! result =
                updateUsername.ExecuteAsync(
                    { AccountId = Ulid.newOf request.AccountId
                      Username = NonEmptyString100.newOf request.Username }
                )

            return
                match result with
                | Ok _ -> Results.NoContent()
                | Error _ -> Results.InternalServerError()
        }
