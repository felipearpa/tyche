namespace Felipearpa.Tyche.Pool.Infrastructure

open System
open System.Collections.Generic
open System.Linq
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.DataModel
open Amazon.DynamoDBv2.DocumentModel
open Amazon.DynamoDBv2.Model
open Felipearpa.Core
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Data
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type PoolGamblerBetDynamoDbRepository(keySerializer: IKeySerializer, client: IAmazonDynamoDB) =
    [<Literal>]
    let tableName = "Pool"

    [<Literal>]
    let poolText = "POOL"

    [<Literal>]
    let gamblerText = "GAMBLER"

    [<Literal>]
    let matchText = "MATCH"

    let context = new DynamoDBContext(client)

    let map (dictionary: IDictionary<string, AttributeValue>) =
        context.FromDocument<PoolGamblerBetEntity>(Document.FromAttributeMap(Dictionary(dictionary)))

    interface IPoolGamblerBetRepository with
        member this.GetPendingPoolGamblerBets(poolId, gamblerId, maybeSearchText, maybeNext) =
            async {
                let keyConditionExpression = "#pk = :pk"

                let defaultFilterConditionExpression = ":now < #matchDateTime"

                let defaultAttributeValues =
                    dict
                        [ ":pk",
                          AttributeValue($"{gamblerText}#{gamblerId |> Ulid.value}#{poolText}#{poolId |> Ulid.value}")
                          ":now", AttributeValue(DateTime.Now.ToUniversalTime().ToString("o")) ]

                let defaultAttributeNames = dict [ "#pk", "pk"; "#matchDateTime", "matchDateTime" ]

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
                        IndexName = "GetPendingPoolGamblerBets-index",
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
                    { CursorPage.Items = response.Items.Select(map >> PoolGamblerBetMapper.mapToDomain)
                      Next =
                        match lastEvaluatedKey.Count with
                        | 0 -> None
                        | _ -> keySerializer.Serialize(lastEvaluatedKey) |> Some }
            }

        member this.GetFinishedPoolGamblerBets(poolId, gamblerId, maybeSearchText, maybeNext) =
            async {
                let keyConditionExpression = "#pk = :pk"

                let defaultFilterConditionExpression = ":now >= #matchDateTime"

                let defaultAttributeValues =
                    dict
                        [ ":pk",
                          AttributeValue($"{gamblerText}#{gamblerId |> Ulid.value}#{poolText}#{poolId |> Ulid.value}")
                          ":now", AttributeValue(DateTime.Now.ToUniversalTime().ToString("o")) ]

                let defaultAttributeNames = dict [ "#pk", "pk"; "#matchDateTime", "matchDateTime" ]

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
                        IndexName = "GetPendingPoolGamblerBets-index",
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
                    { CursorPage.Items = response.Items.Select(map >> PoolGamblerBetMapper.mapToDomain)
                      Next =
                        match lastEvaluatedKey.Count with
                        | 0 -> None
                        | _ -> keySerializer.Serialize(lastEvaluatedKey) |> Some }
            }

        member this.BetAsync(poolId, gamblerId, matchId, betScore) =
            async {
                let key =
                    dict
                        [ "pk",
                          AttributeValue($"{gamblerText}#{gamblerId |> Ulid.value}#{poolText}#{poolId |> Ulid.value}")
                          "sk", AttributeValue($"{matchText}#{matchId |> Ulid.value}") ]

                let updateExpression =
                    "SET #homeTeamBet = :homeTeamBet, #awayTeamBet = :awayTeamBet"

                let conditionExpression = ":now < #matchDateTime"

                let mutable attributeNames =
                    dict
                        [ "#homeTeamBet", "homeTeamBet"
                          "#awayTeamBet", "awayTeamBet"
                          "#matchDateTime", "matchDateTime" ]

                let mutable attributeValues =
                    dict
                        [ ":homeTeamBet", AttributeValue(N = betScore.HomeTeamValue.ToString())
                          ":awayTeamBet", AttributeValue(N = betScore.AwayTeamValue.ToString())
                          ":now", AttributeValue(S = DateTime.Now.ToUniversalTime().ToString("o")) ]

                let request =
                    UpdateItemRequest(
                        TableName = tableName,
                        Key = Dictionary key,
                        ExpressionAttributeNames = Dictionary attributeNames,
                        ExpressionAttributeValues = Dictionary attributeValues,
                        UpdateExpression = updateExpression,
                        ConditionExpression = conditionExpression,
                        ReturnValues = "ALL_NEW"
                    )

                return!
                    async {
                        try
                            let! response = client.UpdateItemAsync(request) |> Async.AwaitTask
                            return map response.Attributes |> PoolGamblerBetMapper.mapToDomain |> Ok
                        with :? AggregateException as error when
                            (error.InnerException :? ConditionalCheckFailedException) ->
                            return Error BetFailure.MatchLocked
                    }
            }
