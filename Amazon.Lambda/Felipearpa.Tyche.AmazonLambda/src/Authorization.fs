namespace Felipearpa.Tyche.AmazonLambda

open Amazon.Lambda.APIGatewayEvents
open Felipearpa.Tyche.Account.Domain
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type AuthFailure =
    | MissingClaim
    | UnknownAccount
    | NotAMember

module Authorization =

    let private emailClaim = "email"

    let private tryGetClaim (claim: string) (request: APIGatewayHttpApiV2ProxyRequest) =
        request.RequestContext
        |> Option.ofObj
        |> Option.bind (fun ctx -> ctx.Authorizer |> Option.ofObj)
        |> Option.bind (fun authorizer -> authorizer.Jwt |> Option.ofObj)
        |> Option.bind (fun jwt -> jwt.Claims |> Option.ofObj)
        |> Option.bind (fun claims ->
            match claims.TryGetValue claim with
            | true, value when value <> null && value <> "" -> Some value
            | _ -> None)

    let resolveCallerGamblerIdAsync
        (request: APIGatewayHttpApiV2ProxyRequest)
        (accountRepository: IAccountRepository)
        : Async<Result<Ulid, AuthFailure>> =
        async {
            match tryGetClaim emailClaim request with
            | None -> return Error MissingClaim
            | Some emailValue ->
                let! accountResult = accountRepository.GetByEmailAsync(Email.newOf emailValue)

                return
                    match accountResult with
                    | Ok(Some account) -> Ok account.AccountId
                    | Ok None -> Error UnknownAccount
                    | Error _ -> Error UnknownAccount
        }

    let requirePoolAccessAsync
        (request: APIGatewayHttpApiV2ProxyRequest)
        (poolId: Ulid)
        (targetGamblerId: Ulid)
        (accountRepository: IAccountRepository)
        (poolRepository: IPoolRepository)
        : Async<Result<Ulid, AuthFailure>> =
        async {
            let! callerResult = resolveCallerGamblerIdAsync request accountRepository

            match callerResult with
            | Error failure -> return Error failure
            | Ok callerGamblerId when callerGamblerId.Value = targetGamblerId.Value -> return Ok callerGamblerId
            | Ok callerGamblerId ->
                let! membership = poolRepository.IsPoolMemberAsync(poolId, callerGamblerId)

                return
                    match membership with
                    | Ok true -> Ok callerGamblerId
                    | Ok false -> Error NotAMember
                    | Error _ -> Error NotAMember
        }

    let toResponse (failure: AuthFailure) : APIGatewayHttpApiV2ProxyResponse =
        match failure with
        | MissingClaim -> UnauthorizedResponseFactory.create ()
        | UnknownAccount -> UnauthorizedResponseFactory.create ()
        | NotAMember -> ForbiddenResponseFactory.create ()
