namespace Felipearpa.Tyche.HttpApi

open System
open System.Security.Claims
open Felipearpa.Tyche.Account.Domain
open Felipearpa.Type

module CallerResolver =

    [<Literal>]
    let private emailClaim = "email"

    let private tryGetEmail (user: ClaimsPrincipal) =
        [ emailClaim; ClaimTypes.Email ]
        |> List.tryPick (fun claimType ->
            match user.FindFirst claimType |> Option.ofObj with
            | Some claim when not (String.IsNullOrWhiteSpace claim.Value) -> Some claim.Value
            | _ -> None)

    /// Resolves the authenticated caller's gambler id (== account id) from the JWT `email` claim.
    let resolveCallerGamblerIdAsync
        (user: ClaimsPrincipal)
        (accountRepository: IAccountRepository)
        : Async<Result<Ulid, unit>> =
        async {
            match tryGetEmail user with
            | None -> return Error()
            | Some emailValue ->
                let! accountResult = accountRepository.GetByEmailAsync(Email.newOf emailValue)

                return
                    match accountResult with
                    | Ok(Some account) -> Ok account.AccountId
                    | _ -> Error()
        }
