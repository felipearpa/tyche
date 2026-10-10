namespace Felipearpa.Tyche.AmazonLambda.Event

#nowarn "3536"

open System.Threading.Tasks
open Amazon.Lambda.Core
open Amazon.Lambda.DynamoDBEvents
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Type
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging

/// Classifies committed membership records from the Pool stream.
module MembershipEventFilter =

    let tryChange (record: DynamoDBEvent.DynamodbStreamRecord) : MembershipChange option =
        match StreamBatch.tryMembershipKey record with
        | None -> None
        | Some(poolId, gamblerId) ->
            let poolId = Ulid.newOf poolId
            let gamblerId = Ulid.newOf gamblerId

            match record.EventName with
            | "INSERT" -> Some(MembershipCreated(poolId, gamblerId))
            | "REMOVE" -> Some(MembershipRemoved(poolId, gamblerId))
            | "MODIFY" ->
                let oldImage = record.Dynamodb.OldImage
                let newImage = record.Dynamodb.NewImage
                let version image = StreamBatch.imageNumber "scoreVersion" image
                let generation image = StreamBatch.imageString "membershipGeneration" image

                // Position, before-score, and username writes keep the version and generation,
                // so they never request propagation.
                if version newImage <> version oldImage || generation newImage <> generation oldImage then
                    Some(MembershipTotalChanged(poolId, gamblerId))
                else
                    None
            | _ -> None

/// Turns committed membership changes into durable standings reconciliation and total
/// propagation. The event source mapping filters the stream to membership records.
type MembershipEvent(configureServices: IServiceCollection -> unit) =

    let serviceProvider = PoolServices.build configureServices

    new() = MembershipEvent(fun _ -> ())

    member this.OnMembershipChangeAsync(event: DynamoDBEvent, _: ILambdaContext) : Task<StreamsEventResponse> =
        let react = serviceProvider.GetRequiredService<ReactToMembershipChanges>()
        let logger = serviceProvider.GetRequiredService<ILogger<MembershipEvent>>()

        let records = event.Records |> Option.ofObj |> Option.map List.ofSeq |> Option.defaultValue []

        let changes =
            records
            |> List.choose (fun record -> MembershipEventFilter.tryChange record |> Option.map (fun change -> change, record.EventID))

        async {
            let! outcome = react.ExecuteAsync changes |> Async.Catch

            match outcome, records with
            | Choice1Of2(), _
            | _, [] -> return StreamsEventResponse(BatchItemFailures = ResizeArray())
            | Choice2Of2 error, first :: _ ->
                // Publishing is idempotent per pool command and version guarded per job, so the
                // whole delivery is retried.
                logger.LogError(error, "Membership changes could not be enqueued")
                OperationMetrics.emit "MembershipEvent" [ OperationMetrics.count "StreamRecordFailures" records.Length ] []

                return
                    StreamsEventResponse(
                        BatchItemFailures =
                            ResizeArray([ StreamsEventResponse.BatchItemFailure(ItemIdentifier = first.Dynamodb.SequenceNumber) ])
                    )
        }
        |> Async.StartAsTask
