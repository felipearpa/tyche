namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open System.Linq
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.DataModel
open Amazon.DynamoDBv2.DocumentModel
open Amazon.DynamoDBv2.Model
open Felipearpa.Core
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type PoolGamblerScoreDynamoDbRepository(keySerializer: IKeySerializer, client: IAmazonDynamoDB) =

    [<Literal>]
    let tableName = "Pool"

    [<Literal>]
    let gamblerText = "GAMBLER"

    [<Literal>]
    let poolText = "POOL"

    let context = new DynamoDBContext(client)

    let map (dictionary: IDictionary<string, AttributeValue>) =
        context.FromDocument<PoolGamblerScoreEntity>(Document.FromAttributeMap(Dictionary(dictionary)))

    interface IPoolGamblerScoreRepository with

        member this.GetPoolGamblerScoresByGamblerAsync(gamblerId, maybeSearchText, maybeNext) =
            async {
                let keyConditionExpression = "#sk = :sk"

                let defaultFilterConditionExpression = "#status = :status"

                let mutable defaultAttributeValues =
                    dict
                        [ ":sk", AttributeValue($"{gamblerText}#{gamblerId |> Ulid.value}")
                          ":status", AttributeValue("OPENED") ]

                let mutable defaultAttributeNames = dict [ "#sk", "sk"; "#status", "status" ]

                let filterExpression, attributeValues, attributeNames =
                    match maybeSearchText with
                    | None -> (defaultFilterConditionExpression, defaultAttributeValues, defaultAttributeNames)
                    | Some filterText ->
                        ($"{defaultFilterConditionExpression} and contains(#filter, :filter)",
                         defaultAttributeValues
                         |> Dict.union (dict [ ":filter", AttributeValue(filterText.ToLower()) ])
                         :> IDictionary<_, _>,
                         defaultAttributeNames |> Dict.union (dict [ "#filter", "filter" ]) :> IDictionary<_, _>)

                let request =
                    QueryRequest(
                        TableName = tableName,
                        IndexName = "GetPoolGamblerScoresByGambler-index",
                        KeyConditionExpression = keyConditionExpression,
                        FilterExpression = filterExpression,
                        ExpressionAttributeNames = Dictionary attributeNames,
                        ExpressionAttributeValues = Dictionary attributeValues,
                        ExclusiveStartKey =
                            match maybeNext with
                            | None -> null
                            | Some next -> Dictionary(next |> keySerializer.Deserialize)
                    )

                let! response = client.QueryAsync(request) |> Async.AwaitTask

                let lastEvaluatedKey = response.LastEvaluatedKey

                return
                    { CursorPage.Items = response.Items.Select(map >> PoolGamblerScoreMapper.mapToDomain)
                      Next =
                        match lastEvaluatedKey.Count with
                        | 0 -> None
                        | _ -> keySerializer.Serialize(lastEvaluatedKey) |> Some }
            }

        member this.GetPoolGamblerScoresByPoolAsync(poolId, maybeSearchText, maybeNext) =
            async {
                let keyConditionExpression = "#pk = :pk"

                let defaultFilterConditionExpression = "#status = :status"

                let mutable defaultAttributeValues =
                    dict
                        [ ":pk", AttributeValue($"{poolText}#{poolId |> Ulid.value}")
                          ":status", AttributeValue("OPENED") ]

                let mutable defaultAttributeNames = dict [ "#pk", "pk"; "#status", "status" ]

                let filterExpression, attributeValues, attributeNames =
                    match maybeSearchText with
                    | None -> (defaultFilterConditionExpression, defaultAttributeValues, defaultAttributeNames)
                    | Some filterText ->
                        ($"{defaultFilterConditionExpression} and contains(#filter, :filter)",
                         defaultAttributeValues
                         |> Dict.union (dict [ ":filter", AttributeValue(filterText.ToLower()) ])
                         :> IDictionary<_, _>,
                         defaultAttributeNames |> Dict.union (dict [ "#filter", "filter" ]) :> IDictionary<_, _>)

                let request =
                    QueryRequest(
                        TableName = tableName,
                        IndexName = "GetPoolGamblerScoresByPool-index",
                        KeyConditionExpression = keyConditionExpression,
                        FilterExpression = filterExpression,
                        ExpressionAttributeNames = Dictionary attributeNames,
                        ExpressionAttributeValues = Dictionary attributeValues,
                        ExclusiveStartKey =
                            match maybeNext with
                            | None -> null
                            | Some next -> Dictionary(next |> keySerializer.Deserialize)
                    )

                let! response = client.QueryAsync(request) |> Async.AwaitTask

                let lastEvaluatedKey = response.LastEvaluatedKey

                return
                    { CursorPage.Items = response.Items.Select(map >> PoolGamblerScoreMapper.mapToDomain)
                      Next =
                        match lastEvaluatedKey.Count with
                        | 0 -> None
                        | _ -> keySerializer.Serialize(lastEvaluatedKey) |> Some }
            }
