namespace Felipearpa.Tyche.HttpApi.Test

#nowarn "3536"

open System
open System.Collections.Generic
open System.Net
open System.Net.Http
open System.Security.Claims
open System.Text.Encodings.Web
open System.Threading
open System.Threading.Tasks
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Account.Domain
open Felipearpa.Tyche.HttpApi
open Felipearpa.Type
open FsUnitTyped
open Microsoft.AspNetCore.Authentication
open Microsoft.AspNetCore.Builder
open Microsoft.AspNetCore.Hosting
open Microsoft.AspNetCore.TestHost
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging
open Microsoft.Extensions.Options
open Moq
open Xunit

/// The HttpApi host's handling of `next` tokens, through its real routes, query binding, and
/// result execution: unusable tokens get 400 `INVALID_CURSOR` before the paginated query
/// runs; DynamoDB failures for a usable token keep their existing handling.
module InvalidCursorTest =

    [<Literal>]
    let private callerEmail = "tester@example.com"

    /// Authenticates every request as the caller, in place of the Firebase JWT bearer scheme.
    type CallerAuthenticationHandler(options: IOptionsMonitor<AuthenticationSchemeOptions>, logger: ILoggerFactory, encoder: UrlEncoder) =
        inherit AuthenticationHandler<AuthenticationSchemeOptions>(options, logger, encoder)

        override this.HandleAuthenticateAsync() =
            let identity = ClaimsIdentity([ Claim("email", callerEmail) ], this.Scheme.Name)
            Task.FromResult(AuthenticateResult.Success(AuthenticationTicket(ClaimsPrincipal identity, this.Scheme.Name)))

    let private poolId = "01K1PX1TX2NM1HG851S1V0P001"
    let private otherPoolId = "01K1PX1TX2NM1HG851S1V0P002"
    let private gamblerId = "01K1PX1TX2NM1HG851S1V0GA01"
    let private otherGamblerId = "01K1PX1TX2NM1HG851S1V0GA02"
    let private matchId = "01K1PX1TX2NM1HG851S1V0M001"
    let private otherMatchId = "01K1PX1TX2NM1HG851S1V0M002"
    let private layoutId = "01K1PX1TX2NM1HG851S1V0Y001"

    let private token (attributes: (string * ScalarKeyValue) list) =
        match ContinuationKey.tryCreate (Map attributes) with
        | Ok key -> ContinuationToken.encode key
        | Error error -> failwith $"invalid test key: %A{error}"

    let private legacy pk sk = $"{{\"pk\":\"{pk}\",\"sk\":\"{sk}\"}}"

    let private betToken pool gambler dateAttribute =
        token
            [ "pk", S $"GAMBLER#{gambler}#POOL#{pool}"
              "sk", S $"MATCH#{matchId}"
              dateAttribute, S "2026-06-12T17:00:00.0000000Z" ]

    let private scoreToken pool gambler =
        token [ "pk", S $"POOL#{pool}"; "sk", S $"GAMBLER#{gambler}"; "score", N "5" ]

    let private memberToken pool =
        token [ "pk", S $"POOL#{pool}"; "sk", S $"GAMBLER#{gamblerId}"; "gamblerUsername", S "bea" ]

    let private matchBetToken matchValue =
        token
            [ "pk", S $"GAMBLER#{gamblerId}#POOL#{poolId}"
              "sk", S $"MATCH#{matchValue}"
              "getPoolGamblerScoresByMatchPk", S $"MATCH#{matchValue}"
              "getPoolGamblerScoresByMatchSk", S $"POOL#{poolId}#GAMBLER#{gamblerId}" ]

    let private layoutToken status =
        token
            [ "pk", S $"POOLLAYOUT#{layoutId}"
              "sk", S $"POOLLAYOUT#{layoutId}"
              "status", S status
              "startDateTime", S "2026-06-11T00:00:00Z" ]

    let private poolRoot =
        Dictionary(
            dict
                [ "pk", AttributeValue(S = $"POOL#{poolId}")
                  "sk", AttributeValue(S = $"POOL#{poolId}")
                  "poolId", AttributeValue(S = poolId)
                  "poolName", AttributeValue(S = "Pool")
                  "poolLayoutId", AttributeValue(S = layoutId)
                  "creatorGamblerId", AttributeValue(S = gamblerId) ]
        )

    let private client (indexFailure: exn option) =
        let mock = Mock<IAmazonDynamoDB>()

        mock
            .Setup(fun c -> c.QueryAsync(It.IsAny<QueryRequest>(), It.IsAny<CancellationToken>()))
            .Returns(fun (request: QueryRequest) (_: CancellationToken) ->
                match request.IndexName, indexFailure with
                | null, _ -> Task.FromResult(QueryResponse(Items = List [ poolRoot ]))
                | _, Some error -> Task.FromException<QueryResponse> error
                | _, None -> Task.FromResult(QueryResponse(Items = List())))
        |> ignore

        mock

    let private indexQueries (mock: Mock<IAmazonDynamoDB>) =
        mock.Invocations
        |> Seq.filter (fun invocation -> invocation.Method.Name = "QueryAsync")
        |> Seq.map (fun invocation -> invocation.Arguments[0] :?> QueryRequest)
        |> Seq.filter (fun query -> not (isNull query.IndexName))
        |> List.ofSeq

    let private accounts () =
        let mock = Mock<IAccountRepository>()

        let account: Account =
            { AccountId = Ulid.newOf gamblerId
              Email = Email.newOf callerEmail
              Username = NonEmptyString100.newOf callerEmail
              ExternalAccountId = NonEmptyString.newOf "external-id" }

        mock.Setup(fun repo -> repo.GetByEmailAsync(It.IsAny<Email>())).Returns(async { return Ok(Some account) })
        |> ignore

        mock.Object

    /// The HttpApi routes with the production dependency registrations, except DynamoDB, the
    /// account lookup, and authentication.
    let private startHost (dynamo: IAmazonDynamoDB) =
        task {
            let builder = WebApplication.CreateBuilder()
            builder.WebHost.UseTestServer() |> ignore
            builder.Logging.ClearProviders() |> ignore
            registerDependencies builder
            builder.Services.AddSingleton<IAmazonDynamoDB>(dynamo) |> ignore
            builder.Services.AddScoped<IAccountRepository>(fun _ -> accounts ()) |> ignore

            builder.Services
                .AddAuthentication("Caller")
                .AddScheme<AuthenticationSchemeOptions, CallerAuthenticationHandler>("Caller", ignore)
            |> ignore

            builder.Services.AddAuthorization() |> ignore

            let app = builder.Build()

            app.ConfigureMiddleware().ConfigurePoolRoutes().ConfigureGamblerRoutes().ConfigurePoolLayoutRoutes()
            |> ignore

            do! app.StartAsync()
            return app
        }

    type private Endpoint =
        { Name: string
          Path: string
          Index: string
          Valid: string
          Invalid: (string * string) list }

    let private bets = $"/pools/{poolId}/gamblers/{gamblerId}/bets"
    let private malformed = "malformed", "not a token"

    let private endpoints: Endpoint list =
        [ { Name = "leaderboard"
            Path = $"/pools/{poolId}/gamblers"
            Index = "GetPoolGamblerScoresByPool-index"
            Valid = scoreToken poolId gamblerId
            Invalid =
              [ malformed
                "unsupported version", """{"v":2,"key":{"pk":{"S":"x"}}}"""
                "legacy, missing score", legacy $"POOL#{poolId}" $"GAMBLER#{gamblerId}"
                "score as a string",
                token [ "pk", S $"POOL#{poolId}"; "sk", S $"GAMBLER#{gamblerId}"; "score", S "5" ]
                "another pool", scoreToken otherPoolId gamblerId ] }
          { Name = "pending bets"
            Path = $"{bets}/pending"
            Index = "GetPendingPoolGamblerBets-index"
            Valid = betToken poolId gamblerId "matchDateTime"
            Invalid =
              [ malformed
                "legacy, missing matchDateTime", legacy $"GAMBLER#{gamblerId}#POOL#{poolId}" $"MATCH#{matchId}"
                "another gambler", betToken poolId otherGamblerId "matchDateTime" ] }
          { Name = "live bets"
            Path = $"{bets}/live"
            Index = "GetPendingPoolGamblerBets-index"
            Valid = betToken poolId gamblerId "matchDateTime"
            Invalid =
              [ malformed
                "finished key", betToken poolId gamblerId "computedDateTime"
                "another pool", betToken otherPoolId gamblerId "matchDateTime" ] }
          { Name = "finished bets"
            Path = $"{bets}/finished"
            Index = "GetFinishedPoolGamblerBetsByMatchDate-index"
            Valid = betToken poolId gamblerId "computedMatchDateTime"
            Invalid =
              [ malformed
                "legacy, missing computedMatchDateTime", legacy $"GAMBLER#{gamblerId}#POOL#{poolId}" $"MATCH#{matchId}"
                "computation-time index", betToken poolId gamblerId "computedDateTime"
                "another gambler", betToken poolId otherGamblerId "computedMatchDateTime" ] }
          { Name = "timeline"
            Path = $"{bets}/timeline"
            Index = "GetFinishedPoolGamblerBetsByMatchDate-index"
            Valid = "finished:" + betToken poolId gamblerId "computedMatchDateTime"
            Invalid =
              [ "live, malformed", "live:not a token"
                "live, legacy", "live:" + legacy $"GAMBLER#{gamblerId}#POOL#{poolId}" $"MATCH#{matchId}"
                "finished, computation-time index", "finished:" + betToken poolId gamblerId "computedDateTime"
                "finished, another pool", "finished:" + betToken otherPoolId gamblerId "computedMatchDateTime"
                "unprefixed finished key", betToken poolId gamblerId "computedMatchDateTime" ] }
          { Name = "legacy match bets"
            Path = $"/pools/{poolId}/matches/{matchId}/bets"
            Index = "GetPoolGamblerScoresByMatch-index"
            Valid = matchBetToken matchId
            Invalid =
              [ malformed
                "legacy, missing index keys", legacy $"GAMBLER#{gamblerId}#POOL#{poolId}" $"MATCH#{matchId}"
                "another match", matchBetToken otherMatchId ] }
          { Name = "gambler pools"
            Path = $"/gamblers/{gamblerId}/pools"
            Index = "GetPoolGamblerScoresByGambler-index"
            Valid = scoreToken poolId gamblerId
            Invalid =
              [ malformed
                "legacy, missing score", legacy $"POOL#{poolId}" $"GAMBLER#{gamblerId}"
                "another gambler", scoreToken poolId otherGamblerId ] }
          { Name = "pool members"
            Path = $"/pools/{poolId}/members"
            Index = "GetPoolGamblersByUsername-index"
            Valid = memberToken poolId
            Invalid =
              [ malformed
                "legacy, missing username", legacy $"POOL#{poolId}" $"GAMBLER#{gamblerId}"
                "another pool", memberToken otherPoolId ] }
          { Name = "open layouts"
            Path = "/pool-layouts/open"
            Index = "GetOpenedPoolLayout-index"
            Valid = layoutToken "OPENED"
            Invalid =
              [ malformed
                "legacy, missing status and start date", legacy $"POOLLAYOUT#{layoutId}" $"POOLLAYOUT#{layoutId}"
                "closed status", layoutToken "CLOSED" ] } ]

    let private endpoint name =
        endpoints |> List.find (fun endpoint -> endpoint.Name = name)

    let endpointNames: TheoryData<string> = TheoryData<string>(endpoints |> List.map _.Name)

    /// Sends GET `path?next=<token>`, URL-encoding the token as clients do.
    let private get (dynamo: IAmazonDynamoDB) (path: string) (next: string) =
        task {
            let! app = startHost dynamo

            try
                use http = app.GetTestClient()
                let! response = http.GetAsync($"{path}?next={Uri.EscapeDataString next}")
                let! body = response.Content.ReadAsStringAsync()
                return response.StatusCode, body
            finally
                app.StopAsync().Wait()
        }

    [<Theory>]
    [<MemberData(nameof endpointNames)>]
    let ``an unusable token is a 400 INVALID_CURSOR and the paginated query never runs`` (name: string) =
        task {
            let endpoint = endpoint name

            for case, next in endpoint.Invalid do
                let mock = client None
                let! status, body = get mock.Object endpoint.Path next

                (case, status) |> shouldEqual (case, HttpStatusCode.BadRequest)

                body
                |> shouldEqual
                    """{"error":"INVALID_CURSOR","message":"The continuation is not valid; restart from the first page"}"""

                indexQueries mock |> shouldEqual []
        }

    [<Theory>]
    [<MemberData(nameof endpointNames)>]
    let ``a usable token reaches the paginated query`` (name: string) =
        task {
            let endpoint = endpoint name
            let mock = client None
            let! status, _ = get mock.Object endpoint.Path endpoint.Valid

            status |> shouldEqual HttpStatusCode.OK

            let queries = indexQueries mock
            queries |> List.map _.IndexName |> shouldEqual [ endpoint.Index ]
            queries.Head.ExclusiveStartKey |> isNull |> shouldEqual false
        }

    [<Theory>]
    [<MemberData(nameof endpointNames)>]
    let ``a DynamoDB failure for a usable token is not reported as an invalid cursor`` (name: string) =
        task {
            let endpoint = endpoint name
            let mock = client (Some(AmazonDynamoDBException "Throughput exceeds the current capacity"))

            // TestServer surfaces an unhandled exception to the caller; Kestrel answers it with 500.
            let! outcome = get mock.Object endpoint.Path endpoint.Valid |> Async.AwaitTask |> Async.Catch

            match outcome with
            | Choice1Of2(status, body) -> failwith $"expected the request to fail, got {status} {body}"
            | Choice2Of2 error -> error.GetBaseException() :? AmazonDynamoDBException |> shouldEqual true
        }
