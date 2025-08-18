namespace Felipearpa.Tyche.AmazonLambda.Event

#nowarn "3536"

open System.Threading.Tasks
open Amazon.DynamoDBv2
open Amazon.Lambda.Core
open Amazon.Lambda.DynamoDBEvents
open Felipearpa.Core
open Felipearpa.Core.Json
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Tyche.PoolLayout.Application
open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Tyche.PoolLayout.Infrastructure
open Felipearpa.Type
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging

type PoolEvent(configureServices: IServiceCollection -> unit) =
    [<Literal>]
    let poolKeyPrefix = "POOL"

    [<Literal>]
    let gamblerKeyPrefix = "GAMBLER"

    [<Literal>]
    let insertEventName = "INSERT"

    [<Literal>]
    let poolIdField = "poolId"

    [<Literal>]
    let gamblerIdField = "gamblerId"

    [<Literal>]
    let poolLayoutIdField = "poolLayoutId"

    [<Literal>]
    let poolLayoutVersionField = "poolLayoutVersion"

    [<Literal>]
    let pkField = "pk"

    [<Literal>]
    let skField = "sk"

    let buildServiceProvider () =
        let services = ServiceCollection()

        services
            .AddAWSService<IAmazonDynamoDB>()
            .AddLogging(fun builder -> builder.AddLambdaLogger() |> ignore)
            .AddSingleton<ISerializer, JsonSerializer>()
            .AddSingleton<IKeySerializer, DynamoDbKeySerializer>()
            .AddScoped<IPoolLayoutRepository, PoolLayoutDynamoDbRepository>()
            .AddScoped<IPoolGamblerBetRepository, PoolGamblerBetDynamoDbRepository>()
            .AddScoped<GetPendingPoolLayoutMatchesQuery>()
            .AddScoped<AddMatchCommand>()
        |> ignore

        configureServices services

        services.BuildServiceProvider()

    let serviceProvider = buildServiceProvider ()

    let extractInsertedPoolGamblerRecords (event: DynamoDBEvent) =
        event.Records
        |> Seq.filter (fun record -> record.EventName = insertEventName)
        |> Seq.choose (fun record ->
            let image = record.Dynamodb.NewImage

            let hasRequiredFields =
                image.ContainsKey(poolIdField)
                && image.ContainsKey(gamblerIdField)
                && image.ContainsKey(poolLayoutIdField)
                && image.ContainsKey(poolLayoutVersionField)
                && image.ContainsKey(pkField)
                && image.ContainsKey(skField)

            if not hasRequiredFields then
                None
            else
                let pk = image[pkField].S
                let sk = image[skField].S

                let keyMatches =
                    pk.StartsWith($"{poolKeyPrefix}#") && sk.StartsWith($"{gamblerKeyPrefix}#")

                if keyMatches then
                    Some(
                        image[poolIdField].S |> Ulid.newOf,
                        image[gamblerIdField].S |> Ulid.newOf,
                        image[poolLayoutIdField].S |> Ulid.newOf,
                        image[poolLayoutVersionField].N |> int
                    )
                else
                    None)

    let applyPendingLayoutMatches
        (getPendingPoolLayoutMatchesQuery: GetPendingPoolLayoutMatchesQuery)
        (addMatchCommand: AddMatchCommand)
        (poolId, gamblerId, poolLayoutId, poolLayoutVersion)
        : Async<unit> =
        let rec loop (next: string option) : Async<unit> =
            async {
                let! page = getPendingPoolLayoutMatchesQuery.ExecuteAsync(poolLayoutId, poolLayoutVersion, next)

                let addMatch (poolLayoutMatch: PoolLayoutMatch) : Async<unit> =
                    { InitialPoolGamblerBet.PoolId = poolId
                      GamblerId = gamblerId
                      MatchId = Ulid.random ()
                      PoolLayoutId = poolLayoutId
                      HomeTeamId = poolLayoutMatch.HomeTeamId
                      HomeTeamName = poolLayoutMatch.HomeTeamName
                      AwayTeamId = poolLayoutMatch.AwayTeamId
                      AwayTeamName = poolLayoutMatch.AwayTeamName
                      MatchDateTime = poolLayoutMatch.MatchDateTime
                      PoolLayoutVersion = poolLayoutMatch.PoolLayoutVersion }
                    |> addMatchCommand.ExecuteAsync
                    |> Async.Ignore

                do! page.Items |> Seq.iterAsync addMatch

                match page.Next with
                | Some n -> return! loop (Some n)
                | None -> return ()
            }

        loop None

    new() = PoolEvent(fun _ -> ())

    member this.OnPoolChangedAsync(event: DynamoDBEvent, _: ILambdaContext) : Task =
        (async {
            use scope = serviceProvider.CreateScope()

            let records = extractInsertedPoolGamblerRecords event

            let getPendingPoolLayoutMatchesQuery =
                scope.ServiceProvider.GetRequiredService<GetPendingPoolLayoutMatchesQuery>()

            let addMatchCommand = scope.ServiceProvider.GetRequiredService<AddMatchCommand>()

            do!
                records
                |> Seq.iterAsync (applyPendingLayoutMatches getPendingPoolLayoutMatchesQuery addMatchCommand)

            return ()
         }
         |> Async.StartAsTask
        :> Task)
