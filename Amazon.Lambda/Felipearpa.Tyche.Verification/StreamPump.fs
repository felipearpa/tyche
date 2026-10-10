namespace Felipearpa.Tyche.Verification

open System
open System.Collections.Generic
open Amazon.DynamoDBStreams
open Amazon.DynamoDBStreams.Model
open Amazon.Lambda.DynamoDBEvents

/// Reads a real table stream and converts its records to the Lambda event shape, so the
/// production stream handlers run on genuine stream records. Shards that exist when the pump
/// starts are read from their latest position when startAtLatest is set (skipping seeding
/// writes); shards found later are read from their beginning.
type StreamPump(client: AmazonDynamoDBStreamsClient, streamArn: string, startAtLatest: bool) =

    let iterators = Dictionary<string, string option>()
    let mutable firstRefresh = true

    let rec convert (value: AttributeValue) : DynamoDBEvent.AttributeValue =
        let converted = DynamoDBEvent.AttributeValue()

        if not (isNull value.S) then converted.S <- value.S
        if not (isNull value.N) then converted.N <- value.N
        if value.BOOL.HasValue then converted.BOOL <- value.BOOL
        if value.NULL.HasValue then converted.NULL <- value.NULL

        if not (isNull value.M) then
            converted.M <- Dictionary(value.M |> Seq.map (fun pair -> KeyValuePair(pair.Key, convert pair.Value)))

        if not (isNull value.L) then converted.L <- List(value.L |> Seq.map convert)
        if not (isNull value.SS) then converted.SS <- List(value.SS)
        if not (isNull value.NS) then converted.NS <- List(value.NS)
        converted

    let image (values: Dictionary<string, AttributeValue>) =
        if isNull values then
            null
        else
            Dictionary(values |> Seq.map (fun pair -> KeyValuePair(pair.Key, convert pair.Value)))

    let toEventRecord (record: Record) =
        let stream = DynamoDBEvent.StreamRecord()
        stream.Keys <- image record.Dynamodb.Keys
        stream.NewImage <- image record.Dynamodb.NewImage
        stream.OldImage <- image record.Dynamodb.OldImage
        stream.SequenceNumber <- record.Dynamodb.SequenceNumber
        let converted = DynamoDBEvent.DynamodbStreamRecord()
        converted.EventID <- record.EventID
        converted.EventName <- record.EventName.Value
        converted.EventSourceArn <- streamArn
        converted.Dynamodb <- stream
        converted

    let refreshShards () =
        async {
            let! description =
                client.DescribeStreamAsync(DescribeStreamRequest(StreamArn = streamArn)) |> Async.AwaitTask

            let initialType =
                if firstRefresh && startAtLatest then ShardIteratorType.LATEST else ShardIteratorType.TRIM_HORIZON

            firstRefresh <- false

            // Parents before children keeps per-key order across shard splits.
            for shard in description.StreamDescription.Shards do
                if not (iterators.ContainsKey shard.ShardId) then
                    let! iterator =
                        client.GetShardIteratorAsync(
                            GetShardIteratorRequest(
                                StreamArn = streamArn,
                                ShardId = shard.ShardId,
                                ShardIteratorType = initialType
                            )
                        )
                        |> Async.AwaitTask

                    iterators[shard.ShardId] <- Some iterator.ShardIterator
        }

    /// Returns every record that appeared since the previous poll, in shard order.
    member this.PollAsync() : Async<DynamoDBEvent.DynamodbStreamRecord list> =
        async {
            do! refreshShards ()
            let collected = List<DynamoDBEvent.DynamodbStreamRecord>()

            for shardId in iterators.Keys |> List.ofSeq do
                match iterators[shardId] with
                | None -> ()
                | Some iterator ->
                    let rec drain (current: string) (rounds: int) =
                        async {
                            let! response =
                                client.GetRecordsAsync(GetRecordsRequest(ShardIterator = current, Limit = 1000))
                                |> Async.AwaitTask

                            for record in response.Records do
                                collected.Add(toEventRecord record)

                            match response.NextShardIterator |> Option.ofObj with
                            | Some next when response.Records.Count > 0 && rounds < 50 -> return! drain next (rounds + 1)
                            | next -> return next
                        }

                    let! next = drain iterator 0
                    iterators[shardId] <- next

            return List.ofSeq collected
        }
