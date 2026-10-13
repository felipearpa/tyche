namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Data.DynamoDb.Dictionary
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

/// Reads and guarded rewrites of the kickoff copies on match details.
module DetailKickoffRequestBuilder =

    let private name = ExpressionAttribute.name
    let private s (value: string) = AttributeValue(S = value)

    let private kickoffAttributes =
        [ PoolTable.Attribute.poolId
          PoolTable.Attribute.gamblerId
          PoolTable.Attribute.matchId
          PoolTable.Attribute.matchDateTime
          PoolTable.Attribute.computedMatchDateTime
          PoolTable.Attribute.computedRequestId ]

    /// The match's details on the match index, optionally narrowed to one pool's range.
    let matchDetailsPage (matchId: Ulid) (poolId: Ulid option) (start: Dictionary<string, AttributeValue>) =
        let poolCondition, poolValues =
            match poolId with
            | Some poolId ->
                $" AND begins_with({name PoolTable.Attribute.getPoolGamblerScoresByMatchSk}, :pool)",
                [ ":pool", s $"{KeyPrefix.build PoolTable.Prefix.pool poolId.Value}#" ]
            | None -> "", []

        QueryRequest(
            TableName = PoolTable.name,
            IndexName = PoolTable.Index.scoresByMatch,
            KeyConditionExpression = $"{name PoolTable.Attribute.getPoolGamblerScoresByMatchPk} = :match{poolCondition}",
            ProjectionExpression = (kickoffAttributes |> List.map name |> String.concat ", "),
            ExpressionAttributeNames =
                Dictionary(
                    ExpressionAttribute.names (
                        [ PoolTable.Attribute.getPoolGamblerScoresByMatchPk ]
                        @ (if poolId.IsSome then [ PoolTable.Attribute.getPoolGamblerScoresByMatchSk ] else [])
                        @ kickoffAttributes
                    )
                ),
            ExpressionAttributeValues =
                Dictionary(dict ([ ":match", s (KeyPrefix.build PoolTable.Prefix.match' matchId.Value) ] @ poolValues)),
            ExclusiveStartKey = start
        )

    let getDetail (poolId: Ulid) (gamblerId: Ulid) (matchId: Ulid) =
        GetItemRequest(
            TableName = PoolTable.name,
            Key = PoolSettlementRequestBuilder.detailKey poolId gamblerId matchId,
            ConsistentRead = true
        )

    let toDetailKickoff (item: IDictionary<string, AttributeValue>) : DetailKickoff =
        { PoolId = item[PoolTable.Attribute.poolId].S |> Ulid.newOf
          GamblerId = item[PoolTable.Attribute.gamblerId].S |> Ulid.newOf
          MatchId = item[PoolTable.Attribute.matchId].S |> Ulid.newOf
          MatchDateTime = item[PoolTable.Attribute.matchDateTime].S
          ComputedMatchDateTime =
            item
            |> tryGetAttributeValueOrNone PoolTable.Attribute.computedMatchDateTime
            |> Option.map _.S
          IsComputed = item |> tryGetAttributeValueOrNone PoolTable.Attribute.computedRequestId |> Option.isSome }

    /// Sets the planned copies, conditioned on the detail still holding the copies they were
    /// planned from. A finished-feed copy is only ever written on a computed detail.
    let rewrite (write: KickoffCopyWrite) =
        let expected = write.Expected

        let computedExpectation, computedExpectationValues =
            match expected.ComputedMatchDateTime with
            | Some stored -> $"{name PoolTable.Attribute.computedMatchDateTime} = :expectedComputed", [ ":expectedComputed", s stored ]
            | None -> $"attribute_not_exists({name PoolTable.Attribute.computedMatchDateTime})", []

        let computedAssignment, computedValues, computedGuard =
            match write.ComputedMatchDateTime with
            | Some stored ->
                $", {name PoolTable.Attribute.computedMatchDateTime} = :computed",
                [ ":computed", s stored ],
                $" AND attribute_exists({name PoolTable.Attribute.computedRequestId})"
            | None -> "", [], ""

        UpdateItemRequest(
            TableName = PoolTable.name,
            Key = PoolSettlementRequestBuilder.detailKey expected.PoolId expected.GamblerId expected.MatchId,
            UpdateExpression = $"SET {name PoolTable.Attribute.matchDateTime} = :matchDateTime{computedAssignment}",
            ConditionExpression =
                $"attribute_exists({name Key.pk}) AND {name PoolTable.Attribute.matchDateTime} = :expectedMatchDateTime AND {computedExpectation}{computedGuard}",
            ExpressionAttributeNames =
                Dictionary(
                    ExpressionAttribute.names (
                        [ Key.pk; PoolTable.Attribute.matchDateTime; PoolTable.Attribute.computedMatchDateTime ]
                        @ (if write.ComputedMatchDateTime.IsSome then [ PoolTable.Attribute.computedRequestId ] else [])
                    )
                ),
            ExpressionAttributeValues =
                Dictionary(
                    dict (
                        [ ":matchDateTime", s write.MatchDateTime
                          ":expectedMatchDateTime", s expected.MatchDateTime ]
                        @ computedExpectationValues
                        @ computedValues
                    )
                )
        )
