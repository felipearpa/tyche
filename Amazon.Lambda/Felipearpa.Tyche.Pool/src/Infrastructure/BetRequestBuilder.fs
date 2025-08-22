namespace Felipearpa.Tyche.Pool.Infrastructure

open System
open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type

module BetRequestBuilder =

    [<Literal>]
    let tableName = "Pool"

    [<Literal>]
    let poolKeyPrefix = "POOL"

    [<Literal>]
    let gamblerKeyPrefix = "GAMBLER"

    [<Literal>]
    let matchKeyPrefix = "MATCH"

    let build (poolId: Ulid) (gamblerId: Ulid) (matchId: Ulid) (betScore: TeamScore<BetScore>) =
        let key =
            dict
                [ "pk", AttributeValue($"{gamblerKeyPrefix}#{gamblerId}#{poolKeyPrefix}#{poolId}")
                  "sk", AttributeValue($"{matchKeyPrefix}#{matchId}") ]

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

        UpdateItemRequest(
            TableName = tableName,
            Key = Dictionary key,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            UpdateExpression = updateExpression,
            ConditionExpression = conditionExpression,
            ReturnValues = "ALL_NEW"
        )
