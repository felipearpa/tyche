namespace Felipearpa.Tyche.MatchScoreIngestion.Infrastructure

#nowarn "3536"

open System
open Amazon.DynamoDBv2
open Felipearpa.Tyche.MatchScoreIngestion.Domain
open Felipearpa.Type

type MatchScoreIngestionDynamoDbRepository(client: IAmazonDynamoDB) =
    interface IMatchScoreIngestionRepository with
        member _.RecordPollAsync(matchId: Ulid) =
            async {
                let request = RecordPollRequestBuilder.build matchId DateTime.UtcNow
                let! _ = client.UpdateItemAsync request |> Async.AwaitTask
                return ()
            }

        member _.MarkCompletedAsync(matchId: Ulid) =
            async {
                let request = MarkCompletedRequestBuilder.build matchId DateTime.UtcNow
                let! _ = client.UpdateItemAsync request |> Async.AwaitTask
                return ()
            }

        member _.MarkExpiredAsync(matchId: Ulid) =
            async {
                let request = MarkExpiredRequestBuilder.build matchId
                let! _ = client.UpdateItemAsync request |> Async.AwaitTask
                return ()
            }
