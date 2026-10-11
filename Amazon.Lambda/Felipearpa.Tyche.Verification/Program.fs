namespace Felipearpa.Tyche.Verification

#nowarn "3536"

open System
open System.IO
open System.Text.Json
open Amazon
open Amazon.DynamoDBStreams
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Amazon.SQS

/// Usage: dotnet run -- integration|load|latency|pagination <results.json> [members matches viewers]
/// The resource environment variables are described in README.md.
module Program =

    let private streamArn (dynamo: IAmazonDynamoDB) (table: string) =
        (dynamo.DescribeTableAsync(DescribeTableRequest(TableName = table)) |> Async.AwaitTask |> Async.RunSynchronously)
            .Table.LatestStreamArn

    let private region () =
        RegionEndpoint.GetBySystemName(Environment.GetEnvironmentVariable "AWS_REGION" |> Option.ofObj |> Option.defaultValue "us-east-2")

    /// Needs only the two tables; no streams or queues.
    let private runPagination (output: string) =
        VerificationEnvironment.assertIsolatedTables ()
        use client = new PageLimitedDynamoDbClient(region (), Pagination.pageLimit)
        let checks = Pagination.runAsync client |> Async.RunSynchronously
        File.WriteAllText(output, JsonSerializer.Serialize(checks, JsonSerializerOptions(WriteIndented = true)))
        if checks |> List.forall _.Passed then 0 else 1

    let private runPipeline (argv: string array) =
        VerificationEnvironment.assertIsolated ()
        let region = region ()
        let dynamo = new AmazonDynamoDBClient(region) :> IAmazonDynamoDB
        let sqs = new AmazonSQSClient(region) :> IAmazonSQS
        let streams = new AmazonDynamoDBStreamsClient(region)

        let workers =
            QueueWorkers(sqs, VerificationEnvironment.commandQueueUrl (), VerificationEnvironment.maintenanceQueueUrl ())

        let pipeline =
            Pipeline(
                streams,
                streamArn dynamo (VerificationEnvironment.poolTable ()),
                streamArn dynamo (VerificationEnvironment.poolLayoutTable ()),
                workers,
                true
            )

        let output = argv |> Array.tryItem 1 |> Option.defaultValue "verification-results.json"
        let options = JsonSerializerOptions(WriteIndented = true)

        match argv |> Array.tryHead with
        | Some "integration" ->
            let integration = Integration(dynamo, sqs, pipeline)
            integration.RunAsync() |> Async.RunSynchronously
            File.WriteAllText(output, JsonSerializer.Serialize(integration.Checks, options))
            if integration.Checks |> List.forall _.Passed then 0 else 1
        | Some "load" ->
            let number index fallback = argv |> Array.tryItem index |> Option.map int |> Option.defaultValue fallback
            let load = Load(dynamo, sqs, pipeline, number 2 5000, number 3 100, number 4 1000)
            load.RunAsync() |> Async.RunSynchronously
            File.WriteAllText(output, JsonSerializer.Serialize(load.Measurements, options))
            0
        | Some "latency" ->
            let number index fallback = argv |> Array.tryItem index |> Option.map int |> Option.defaultValue fallback
            let load = Load(dynamo, sqs, pipeline, number 2 5000, 100, 0)
            load.RunLatencyAsync() |> Async.RunSynchronously
            File.WriteAllText(output, JsonSerializer.Serialize(load.Measurements, options))
            0
        | _ ->
            eprintfn "Usage: integration|load|latency|pagination <results.json> [members matches viewers]"
            2

    [<EntryPoint>]
    let main argv =
        match argv |> Array.tryHead with
        | Some "pagination" -> runPagination (argv |> Array.tryItem 1 |> Option.defaultValue "pagination-results.json")
        | _ -> runPipeline argv
