namespace Felipearpa.Tyche.AmazonLambda.Event

#nowarn "3536"

open System.Threading.Tasks
open Amazon.Lambda.Core
open Amazon.Lambda.DynamoDBEvents
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging

/// Pool stream handler for membership lifecycle: a new membership's bet sheet is
/// materialized through the guarded initializer, and a removed membership's generation is
/// retired from its details.
type PoolEvent(configureServices: IServiceCollection -> unit) =

    let serviceProvider = PoolServices.build configureServices

    let tryJoinedGambler (record: DynamoDBEvent.DynamodbStreamRecord) =
        let image = record.Dynamodb.NewImage

        match
            StreamBatch.imageString "poolId" image,
            StreamBatch.imageString "gamblerId" image,
            StreamBatch.imageString "gamblerUsername" image,
            StreamBatch.imageString "gamblerEmail" image,
            StreamBatch.imageString "poolLayoutId" image,
            StreamBatch.imageNumber "poolLayoutVersion" image
        with
        | Some poolId, Some gamblerId, Some username, Some email, Some poolLayoutId, Some poolLayoutVersion ->
            Some
                { PoolLayoutGambler.PoolId = Ulid.newOf poolId
                  PoolLayoutId = Ulid.newOf poolLayoutId
                  PoolLayoutVersion = int poolLayoutVersion
                  GamblerId = Ulid.newOf gamblerId
                  GamblerUsername = NonEmptyString100.newOf username
                  GamblerEmail = NonEmptyString100.newOf email }
        | _ -> None

    new() = PoolEvent(fun _ -> ())

    member this.OnPoolChangeAsync(event: DynamoDBEvent, _: ILambdaContext) : Task<StreamsEventResponse> =
        let materialize = serviceProvider.GetRequiredService<MaterializeJoinedGambler>()
        let cleanUp = serviceProvider.GetRequiredService<CleanUpRemovedMembership>()
        let logger = serviceProvider.GetRequiredService<ILogger<PoolEvent>>()

        let handle (record: DynamoDBEvent.DynamodbStreamRecord) =
            async {
                match record.EventName, StreamBatch.tryMembershipKey record with
                | "INSERT", Some _ ->
                    match tryJoinedGambler record with
                    | Some gambler -> do! materialize.ExecuteAsync(gambler, record.EventID)
                    | None -> logger.LogWarning("Membership insert {EventId} lacks materialization fields", record.EventID)
                | "REMOVE", Some(poolId, gamblerId) ->
                    let generation = StreamBatch.imageString "membershipGeneration" record.Dynamodb.OldImage
                    do! cleanUp.ExecuteAsync(Ulid.newOf poolId, Ulid.newOf gamblerId, generation)
                | _ -> ()
            }

        StreamBatch.processInOrder "PoolEvent" logger handle event |> Async.StartAsTask
