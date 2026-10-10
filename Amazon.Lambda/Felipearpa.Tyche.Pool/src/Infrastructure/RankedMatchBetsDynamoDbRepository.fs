namespace Felipearpa.Tyche.Pool.Infrastructure

#nowarn "3536"

open System.Collections.Generic
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Domain.PoolGamblerBetDictionaryTransformer
open Felipearpa.Type

/// Queries the dedicated ranking index directly; rows carry everything the participant
/// list needs, so no per-row or per-page enrichment happens. A page may take a bounded
/// number of index queries when the service returns fewer items than requested.
type RankedMatchBetsDynamoDbRepository(client: IAmazonDynamoDB) =

    [<Literal>]
    let maxQueriesPerPage = 3

    let request (poolId: Ulid) (matchId: Ulid) (start: Dictionary<string, AttributeValue>) (limit: int) =
        QueryRequest(
            TableName = PoolTable.name,
            IndexName = PoolTable.Index.poolMatchBetsByTotal,
            Limit = limit,
            KeyConditionExpression = $"{ExpressionAttribute.name PoolTable.Attribute.getPoolMatchBetsByTotalPk} = :pk",
            ExpressionAttributeNames = Dictionary(ExpressionAttribute.names [ PoolTable.Attribute.getPoolMatchBetsByTotalPk ]),
            ExpressionAttributeValues = Dictionary(dict [ ":pk", AttributeValue(S = RankingKey.partition poolId matchId) ]),
            ExclusiveStartKey = start
        )

    interface IRankedMatchBetsRepository with

        member this.GetPageAsync(poolId, matchId, cursor, limit) =
            let rec fill (start: Dictionary<string, AttributeValue>) (items: Dictionary<string, AttributeValue> list) queries =
                async {
                    let! response = client.QueryAsync(request poolId matchId start (limit - items.Length)) |> Async.AwaitTask
                    let items = items @ List.ofSeq response.Items

                    match response.LastEvaluatedKey |> Option.ofObj with
                    | Some key when key.Count > 0 && items.Length < limit && queries + 1 < maxQueriesPerPage ->
                        return! fill key items (queries + 1)
                    | Some key when key.Count > 0 -> return items, Some key
                    | _ -> return items, None
                }

            async {
                let start =
                    match cursor with
                    | Some value -> RankedMatchBetsCursor.tryDecode poolId matchId value |> Result.map Some
                    | None -> Ok None

                match start with
                | Error error -> return Error error
                | Ok start ->
                    let! items, lastEvaluatedKey = fill (start |> Option.defaultValue null) [] 0

                    return
                        Ok
                            { CursorPage.Items = items |> List.map toPoolGamblerBet
                              Next = lastEvaluatedKey |> Option.map (RankedMatchBetsCursor.encode poolId matchId) }
            }
