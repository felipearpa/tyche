namespace Felipearpa.Tyche.Pool.Infrastructure

open System
open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

/// Queries of the grouped History and Timeline feeds. Each feed pages one by-kickoff index;
/// a kickoff's details are adjacent there, and an equality query on the same index in the
/// same direction reads a whole group, ending where the paged traversal resumes after it.
module GroupedBetFeedRequestBuilder =

    let private partition (poolId: Ulid) (gamblerId: Ulid) =
        $"{KeyPrefix.build PoolTable.Prefix.gambler gamblerId.Value}#{KeyPrefix.build PoolTable.Prefix.pool poolId.Value}"

    let private name = ExpressionAttribute.name

    /// The start key of the finished-by-kickoff index: the bet's table keys and its kickoff
    /// copy. A token of the computation-time index lacks `computedMatchDateTime` and is rejected.
    let finishedContract (poolId: Ulid) (gamblerId: Ulid) : StartKeyContract =
        Map
            [ Key.pk, StringEqualTo(partition poolId gamblerId)
              Key.sk, StringStartingWith(KeyPrefix.build PoolTable.Prefix.match' "")
              PoolTable.Attribute.computedMatchDateTime, AnyString ]

    /// The table and index keys of a finished detail, as the traversal's start key after it.
    let finishedKeyOf (item: IDictionary<string, AttributeValue>) =
        Dictionary(
            dict
                [ Key.pk, item[Key.pk]
                  Key.sk, item[Key.sk]
                  PoolTable.Attribute.computedMatchDateTime, item[PoolTable.Attribute.computedMatchDateTime] ]
        )

    /// The table and index keys of a detail on the pending index.
    let pendingKeyOf (item: IDictionary<string, AttributeValue>) =
        Dictionary(
            dict
                [ Key.pk, item[Key.pk]
                  Key.sk, item[Key.sk]
                  PoolTable.Attribute.matchDateTime, item[PoolTable.Attribute.matchDateTime] ]
        )

    /// A page of the gambler's computed details, newest kickoff first.
    let finishedPage
        (poolId: Ulid)
        (gamblerId: Ulid)
        (maybeNext: string option)
        (deserialize: string -> IDictionary<string, AttributeValue>)
        =
        QueryRequest(
            TableName = PoolTable.name,
            IndexName = PoolTable.Index.finishedPoolGamblerBetsByMatchDate,
            KeyConditionExpression = $"{name Key.pk} = :pk",
            ExpressionAttributeNames = Dictionary(ExpressionAttribute.names [ Key.pk ]),
            ExpressionAttributeValues = Dictionary(dict [ ":pk", AttributeValue(partition poolId gamblerId) ]),
            ScanIndexForward = false,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> StartKeyContract.startKey (finishedContract poolId gamblerId) deserialize next
        )

    /// Every computed detail of the gambler at one stored kickoff, in the paged order.
    let finishedAt (poolId: Ulid) (gamblerId: Ulid) (storedKickoff: string) (start: Dictionary<string, AttributeValue>) =
        QueryRequest(
            TableName = PoolTable.name,
            IndexName = PoolTable.Index.finishedPoolGamblerBetsByMatchDate,
            KeyConditionExpression = $"{name Key.pk} = :pk AND {name PoolTable.Attribute.computedMatchDateTime} = :kickoff",
            ExpressionAttributeNames =
                Dictionary(ExpressionAttribute.names [ Key.pk; PoolTable.Attribute.computedMatchDateTime ]),
            ExpressionAttributeValues =
                Dictionary(dict [ ":pk", AttributeValue(partition poolId gamblerId); ":kickoff", AttributeValue(storedKickoff) ]),
            ScanIndexForward = false,
            ExclusiveStartKey = start
        )

    /// A page of the gambler's uncomputed details whose predictions are revealed, oldest
    /// kickoff first. Revealing is the existing lock rule, so a whole kickoff group is
    /// revealed at once.
    let unsettledPage
        (poolId: Ulid)
        (gamblerId: Ulid)
        (maybeNext: string option)
        (deserialize: string -> IDictionary<string, AttributeValue>)
        (now: DateTime)
        =
        QueryRequest(
            TableName = PoolTable.name,
            IndexName = PoolTable.Index.pendingPoolGamblerBets,
            KeyConditionExpression = $"{name Key.pk} = :pk AND {name PoolTable.Attribute.matchDateTime} <= :lockHorizon",
            FilterExpression = $"attribute_not_exists({name PoolTable.Attribute.computedRequestId})",
            ExpressionAttributeNames =
                Dictionary(
                    ExpressionAttribute.names
                        [ Key.pk; PoolTable.Attribute.matchDateTime; PoolTable.Attribute.computedRequestId ]
                ),
            ExpressionAttributeValues =
                Dictionary(
                    dict
                        [ ":pk", AttributeValue(partition poolId gamblerId)
                          ":lockHorizon", AttributeValue((now + LockPolicy.lockOffset).ToString("o")) ]
                ),
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next ->
                    StartKeyContract.startKey
                        (GetPendingPoolGamblerBetsRequestBuilder.startKeyContract poolId gamblerId)
                        deserialize
                        next
        )

    /// Every detail of the gambler at one stored kickoff on the pending index, computed or
    /// not, in the unsettled traversal's order.
    let unsettledAt (poolId: Ulid) (gamblerId: Ulid) (storedKickoff: string) (start: Dictionary<string, AttributeValue>) =
        QueryRequest(
            TableName = PoolTable.name,
            IndexName = PoolTable.Index.pendingPoolGamblerBets,
            KeyConditionExpression = $"{name Key.pk} = :pk AND {name PoolTable.Attribute.matchDateTime} = :kickoff",
            ExpressionAttributeNames = Dictionary(ExpressionAttribute.names [ Key.pk; PoolTable.Attribute.matchDateTime ]),
            ExpressionAttributeValues =
                Dictionary(dict [ ":pk", AttributeValue(partition poolId gamblerId); ":kickoff", AttributeValue(storedKickoff) ]),
            ExclusiveStartKey = start
        )
