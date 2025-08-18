namespace Felipearpa.Tyche.Pool.Infrastructure

#nowarn "3536"

open System
open System.Collections.Generic
open System.Linq
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Core
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Domain.PoolGamblerBetDictionaryTransformer
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

    interface IPoolGamblerBetRepository with
        member this.GetPendingPoolGamblerBetsAsync(poolId, gamblerId, maybeSearchText, maybeNext) =
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
                         |> Dictionary.union (dict [ ":filter", AttributeValue(filterText.ToLower()) ])
                         :> IDictionary<_, _>,
                         defaultAttributeNames |> Dictionary.union (dict [ "#filter", "filter" ]) :> IDictionary<_, _>)

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

                let maybeLastEvaluatedKey = response.LastEvaluatedKey |> Option.ofObj

                return
                    { CursorPage.Items = response.Items.Select(toPoolGamblerBet)
                      Next =
                        match maybeLastEvaluatedKey with
                        | Some lastEvaluatedKey -> keySerializer.Serialize(lastEvaluatedKey) |> Some
                        | None -> None }
            }

        member this.GetFinishedPoolGamblerBetsAsync(poolId, gamblerId, maybeSearchText, maybeNext) =
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
                         |> Dictionary.union (dict [ ":filter", AttributeValue(filterText.ToLower()) ])
                         :> IDictionary<_, _>,
                         defaultAttributeNames |> Dictionary.union (dict [ "#filter", "filter" ]) :> IDictionary<_, _>)

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

                let maybeLastEvaluatedKey = response.LastEvaluatedKey |> Option.ofObj

                return
                    { CursorPage.Items = response.Items.Select(toPoolGamblerBet)
                      Next =
                        match maybeLastEvaluatedKey with
                        | Some lastEvaluatedKey -> keySerializer.Serialize(lastEvaluatedKey) |> Some
                        | None -> None }
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

                try
                    let! response = client.UpdateItemAsync(request) |> Async.AwaitTask
                    return response.Attributes |> toPoolGamblerBet |> Ok
                with :? AggregateException as error when (error.InnerException :? ConditionalCheckFailedException) ->
                    return Error BetFailure.MatchLocked
            }

        member this.AddMatch(poolGamblerBet) =
            async {
                let conditionExpression = "attribute_not_exists(pk) AND attribute_not_exists(sk)"

                let item =
                    dict
                        [ "pk",
                          AttributeValue(
                              S = $"{gamblerText}#{poolGamblerBet.GamblerId}#{poolText}#{poolGamblerBet.PoolId}"
                          )
                          "sk", AttributeValue(S = $"{matchText}#{poolGamblerBet.MatchId}")
                          "poolId", AttributeValue(S = poolGamblerBet.PoolId.Value)
                          "gamblerId", AttributeValue(S = poolGamblerBet.GamblerId.Value)
                          "matchId", AttributeValue(S = poolGamblerBet.MatchId.Value)
                          "homeTeamId", AttributeValue(S = poolGamblerBet.HomeTeamId.Value)
                          "homeTeamName", AttributeValue(S = poolGamblerBet.HomeTeamName.Value)
                          "awayTeamId", AttributeValue(S = poolGamblerBet.AwayTeamId.Value)
                          "awayTeamName", AttributeValue(S = poolGamblerBet.AwayTeamName.Value)
                          "matchDateTime",
                          AttributeValue(S = poolGamblerBet.MatchDateTime.ToUniversalTime().ToString("o"))
                          "poolLayoutId", AttributeValue(S = poolGamblerBet.PoolLayoutId.Value)
                          "poolLayoutVersion", AttributeValue(N = poolGamblerBet.PoolLayoutVersion.ToString()) ]

                let request =
                    PutItemRequest(
                        TableName = tableName,
                        Item = Dictionary item,
                        ConditionExpression = conditionExpression
                    )

                try
                    let! _ = client.PutItemAsync(request) |> Async.AwaitTask
                    return item |> toPoolGamblerBet |> Ok
                with :? AggregateException as error when (error.InnerException :? ConditionalCheckFailedException) ->
                    return Error AddMatchFailure.AlreadyExist
            }
