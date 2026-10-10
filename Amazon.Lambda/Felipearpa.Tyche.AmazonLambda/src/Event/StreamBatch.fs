namespace Felipearpa.Tyche.AmazonLambda.Event

open System.Collections.Generic
open Amazon.Lambda.DynamoDBEvents
open Microsoft.Extensions.Logging

/// Processes stream records in order and reports the first failure by sequence number, so
/// the event source retries from that record without repeating earlier ones and without
/// letting later records of the shard overtake it.
module StreamBatch =

    let processInOrder
        (handler: string)
        (logger: ILogger)
        (process: DynamoDBEvent.DynamodbStreamRecord -> Async<unit>)
        (event: DynamoDBEvent)
        : Async<StreamsEventResponse> =
        let rec loop (records: DynamoDBEvent.DynamodbStreamRecord list) =
            async {
                match records with
                | [] -> return None
                | record :: rest ->
                    let! outcome = process record |> Async.Catch

                    match outcome with
                    | Choice1Of2() -> return! loop rest
                    | Choice2Of2 error ->
                        logger.LogError(error, "Stream record {EventId} failed in {Handler}", record.EventID, handler)
                        OperationMetrics.emit handler [ OperationMetrics.count "StreamRecordFailures" 1 ] [ "eventId", record.EventID ]
                        return Some record.Dynamodb.SequenceNumber
            }

        async {
            let! failed = loop (event.Records |> Option.ofObj |> Option.map List.ofSeq |> Option.defaultValue [])

            return
                StreamsEventResponse(
                    BatchItemFailures =
                        List(
                            failed
                            |> Option.map (fun sequenceNumber ->
                                StreamsEventResponse.BatchItemFailure(ItemIdentifier = sequenceNumber))
                            |> Option.toList
                        )
                )
        }

    let private tryString (key: string) (image: Dictionary<string, DynamoDBEvent.AttributeValue>) =
        match image |> Option.ofObj with
        | Some image ->
            match image.TryGetValue key with
            | true, value when not (isNull value.S) -> Some value.S
            | _ -> None
        | None -> None

    /// The pool and gambler of a membership record, from its keys.
    let tryMembershipKey (record: DynamoDBEvent.DynamodbStreamRecord) =
        let keys = record.Dynamodb.Keys

        match tryString "pk" keys, tryString "sk" keys with
        | Some pk, Some sk when pk.StartsWith "POOL#" && sk.StartsWith "GAMBLER#" ->
            Some(pk.Substring("POOL#".Length), sk.Substring("GAMBLER#".Length))
        | _ -> None

    let imageString key (image: Dictionary<string, DynamoDBEvent.AttributeValue>) = tryString key image

    let imageNumber (key: string) (image: Dictionary<string, DynamoDBEvent.AttributeValue>) =
        match image |> Option.ofObj with
        | Some image ->
            match image.TryGetValue key with
            | true, value when not (isNull value.N) -> Some(int64 value.N)
            | _ -> None
        | None -> None
