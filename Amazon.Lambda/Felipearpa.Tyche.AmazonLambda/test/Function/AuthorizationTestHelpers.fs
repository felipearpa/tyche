namespace Felipearpa.Tyche.AmazonLambda.Function.Tests

open System.Collections.Generic
open Amazon.Lambda.APIGatewayEvents
open Felipearpa.Tyche.Account.Domain
open Felipearpa.Type
open Microsoft.Extensions.DependencyInjection
open Moq

module AuthorizationTestHelpers =

    [<Literal>]
    let testEmail = "tester@example.com"

    let attachJwtClaim (request: APIGatewayHttpApiV2ProxyRequest) (email: string) =
        let jwt = APIGatewayHttpApiV2ProxyRequest.AuthorizerDescription.JwtDescription()
        jwt.Claims <- Dictionary(dict [ "email", email ])

        let authorizer = APIGatewayHttpApiV2ProxyRequest.AuthorizerDescription()
        authorizer.Jwt <- jwt

        let context = APIGatewayHttpApiV2ProxyRequest.ProxyRequestContext()
        context.Authorizer <- authorizer

        request.RequestContext <- context
        request

    let buildAccountRepositoryMockAsCaller (callerGamblerId: string) =
        let mock = Mock<IAccountRepository>()

        let account: Account =
            { AccountId = Ulid.newOf callerGamblerId
              Email = Email.newOf testEmail
              ExternalAccountId = NonEmptyString.newOf "external-id" }

        mock
            .Setup(fun repo -> repo.GetByEmailAsync(It.IsAny<Email>()))
            .Returns(async { return Ok(Some account) })
        |> ignore

        mock

    let registerAuthAsCaller (services: IServiceCollection) (callerGamblerId: string) =
        let accountRepoMock = buildAccountRepositoryMockAsCaller callerGamblerId
        services.AddSingleton<IAccountRepository>(accountRepoMock.Object) |> ignore
