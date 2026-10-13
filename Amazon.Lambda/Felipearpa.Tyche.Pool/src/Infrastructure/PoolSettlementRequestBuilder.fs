namespace Felipearpa.Tyche.Pool.Infrastructure

open System
open System.Collections.Generic
open System.Globalization
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure.PoolSettlementDictionaryTransformer
open Felipearpa.Type

/// Requests for ordered pool operations: admission, checkpoints, completion, authoritative
/// reads, and the guarded settlement and position writes.
module PoolSettlementRequestBuilder =

    let s (value: string) = AttributeValue(S = value)
    let n (value: int64) = AttributeValue(N = value.ToString(CultureInfo.InvariantCulture))
    let ni (value: int) = n (int64 value)

    let poolKey (poolId: Ulid) = KeyPrefix.build PoolTable.Prefix.pool poolId.Value

    let rootKey (poolId: Ulid) =
        Dictionary(dict [ Key.pk, s (poolKey poolId); Key.sk, s (poolKey poolId) ])

    let membershipKey (poolId: Ulid) (gamblerId: Ulid) =
        Dictionary(
            dict
                [ Key.pk, s (poolKey poolId)
                  Key.sk, s (KeyPrefix.build PoolTable.Prefix.gambler gamblerId.Value) ]
        )

    let detailPartition (poolId: Ulid) (gamblerId: Ulid) =
        $"{KeyPrefix.build PoolTable.Prefix.gambler gamblerId.Value}#{poolKey poolId}"

    let detailKey (poolId: Ulid) (gamblerId: Ulid) (matchId: Ulid) =
        Dictionary(
            dict
                [ Key.pk, s (detailPartition poolId gamblerId)
                  Key.sk, s (KeyPrefix.build PoolTable.Prefix.match' matchId.Value) ]
        )

    let receiptKey (poolId: Ulid) (operationId: string) =
        Dictionary(
            dict
                [ Key.pk, s (poolKey poolId)
                  Key.sk, s (KeyPrefix.build PoolTable.Prefix.settlement operationId) ]
        )

    let names (attributes: string list) = Dictionary(ExpressionAttribute.names attributes)

    let values (entries: (string * AttributeValue) list) = Dictionary(dict entries)

    let name = ExpressionAttribute.name

    let getRoot (poolId: Ulid) =
        GetItemRequest(TableName = PoolTable.name, Key = rootKey poolId, ConsistentRead = true)

    let getReceipt (poolId: Ulid) (operationId: string) =
        GetItemRequest(TableName = PoolTable.name, Key = receiptKey poolId operationId, ConsistentRead = true)

    let getMembership (poolId: Ulid) (gamblerId: Ulid) =
        GetItemRequest(TableName = PoolTable.name, Key = membershipKey poolId gamblerId, ConsistentRead = true)

    let getDetail (poolId: Ulid) (gamblerId: Ulid) (matchId: Ulid) =
        GetItemRequest(TableName = PoolTable.name, Key = detailKey poolId gamblerId matchId, ConsistentRead = true)

    let admit (root: PoolSettlementRoot) (receipt: OperationReceipt) (active: ActiveOperation) (now: DateTime) =
        let putReceipt =
            Put(
                TableName = PoolTable.name,
                Item = receiptItem root.PoolId receipt now,
                ConditionExpression = $"attribute_not_exists({name Key.pk}) OR {name PoolTable.Attribute.operationState} = :deferred",
                ExpressionAttributeNames = names [ Key.pk; PoolTable.Attribute.operationState ],
                ExpressionAttributeValues = values [ ":deferred", s (receiptStateValue Deferred) ]
            )

        let revisionCondition =
            if root.OperationRevision = 0L then
                $"(attribute_not_exists({name PoolTable.Attribute.operationRevision}) OR {name PoolTable.Attribute.operationRevision} = :expected)"
            else
                $"{name PoolTable.Attribute.operationRevision} = :expected"

        let installCheckpoint =
            Update(
                TableName = PoolTable.name,
                Key = rootKey root.PoolId,
                UpdateExpression =
                    $"SET {name PoolTable.Attribute.operationRevision} = :revision, {name PoolTable.Attribute.activeOperation} = :active",
                ConditionExpression =
                    $"attribute_exists({name Key.pk}) AND attribute_not_exists({name PoolTable.Attribute.activeOperation}) AND {revisionCondition}",
                ExpressionAttributeNames =
                    names [ Key.pk; PoolTable.Attribute.operationRevision; PoolTable.Attribute.activeOperation ],
                ExpressionAttributeValues =
                    values
                        [ ":revision", n active.Revision
                          ":expected", n root.OperationRevision
                          ":active", activeOperationAttribute active ]
            )

        TransactWriteItemsRequest(
            TransactItems = List([ TransactWriteItem(Put = putReceipt); TransactWriteItem(Update = installCheckpoint) ])
        )

    let recordReceipt (poolId: Ulid) (receipt: OperationReceipt) (now: DateTime) (allowReplacingDeferred: bool) =
        let condition =
            if allowReplacingDeferred then
                $"attribute_not_exists({name Key.pk}) OR {name PoolTable.Attribute.operationState} = :deferred"
            else
                $"attribute_not_exists({name Key.pk})"

        PutItemRequest(
            TableName = PoolTable.name,
            Item = receiptItem poolId receipt now,
            ConditionExpression = condition,
            ExpressionAttributeNames =
                (if allowReplacingDeferred then
                     names [ Key.pk; PoolTable.Attribute.operationState ]
                 else
                     names [ Key.pk ]),
            ExpressionAttributeValues =
                (if allowReplacingDeferred then
                     values [ ":deferred", s (receiptStateValue Deferred) ]
                 else
                     null)
        )

    let getDeferredReceipts (poolId: Ulid) (start: Dictionary<string, AttributeValue>) =
        QueryRequest(
            TableName = PoolTable.name,
            ConsistentRead = true,
            KeyConditionExpression = $"{name Key.pk} = :pk AND begins_with({name Key.sk}, :prefix)",
            FilterExpression = $"{name PoolTable.Attribute.operationState} = :deferred",
            ExpressionAttributeNames = names [ Key.pk; Key.sk; PoolTable.Attribute.operationState ],
            ExpressionAttributeValues =
                values
                    [ ":pk", s (poolKey poolId)
                      ":prefix", s $"{PoolTable.Prefix.settlement}#"
                      ":deferred", s (receiptStateValue Deferred) ],
            ExclusiveStartKey = start
        )

    /// Completed receipts of result settlements (`SETTLEMENT#RESULT#…`).
    let getCompletedResultReceipts (poolId: Ulid) (start: Dictionary<string, AttributeValue>) =
        QueryRequest(
            TableName = PoolTable.name,
            ConsistentRead = true,
            KeyConditionExpression = $"{name Key.pk} = :pk AND begins_with({name Key.sk}, :prefix)",
            FilterExpression = $"{name PoolTable.Attribute.operationState} = :completed",
            ExpressionAttributeNames = names [ Key.pk; Key.sk; PoolTable.Attribute.operationState ],
            ExpressionAttributeValues =
                values
                    [ ":pk", s (poolKey poolId)
                      ":prefix", s $"{PoolTable.Prefix.settlement}#RESULT#"
                      ":completed", s (receiptStateValue Completed) ],
            ExclusiveStartKey = start
        )

    /// Sets the trend kickoff of a root that has none, provided no operation is active and no
    /// operation was admitted since the root was read. An operation records its own trend
    /// kickoff at completion under its owner fence; this write can therefore never overwrite
    /// one, and an operation admitted after it compares with the value it wrote.
    let recordTrendKickoff (root: PoolSettlementRoot) (kickoff: DateTime) =
        let revisionCondition =
            if root.OperationRevision = 0L then
                $"(attribute_not_exists({name PoolTable.Attribute.operationRevision}) OR {name PoolTable.Attribute.operationRevision} = :observed)"
            else
                $"{name PoolTable.Attribute.operationRevision} = :observed"

        UpdateItemRequest(
            TableName = PoolTable.name,
            Key = rootKey root.PoolId,
            UpdateExpression = $"SET {name PoolTable.Attribute.trendKickoffDateTime} = :trendKickoff",
            ConditionExpression =
                $"attribute_exists({name Key.pk}) \
                 AND attribute_not_exists({name PoolTable.Attribute.trendKickoffDateTime}) \
                 AND attribute_not_exists({name PoolTable.Attribute.activeOperation}) \
                 AND {revisionCondition}",
            ExpressionAttributeNames =
                names
                    [ Key.pk
                      PoolTable.Attribute.trendKickoffDateTime
                      PoolTable.Attribute.activeOperation
                      PoolTable.Attribute.operationRevision ],
            ExpressionAttributeValues =
                values [ ":trendKickoff", s (instant kickoff); ":observed", n root.OperationRevision ]
        )

    let private checkpointOwnerCondition =
        $"{name PoolTable.Attribute.activeOperation}.#checkpointOperationId = :operationId AND \
         {name PoolTable.Attribute.activeOperation}.#checkpointOwnerToken = :ownerToken"

    let private checkpointNames =
        let entries =
            ExpressionAttribute.names [ PoolTable.Attribute.activeOperation ]
            |> Seq.map (fun pair -> pair.Key, pair.Value)
            |> List.ofSeq

        entries @ [ "#checkpointOperationId", "operationId"; "#checkpointOwnerToken", "ownerToken" ]

    let replaceCheckpoint (poolId: Ulid) (expectedOwnerToken: string) (next: ActiveOperation) =
        UpdateItemRequest(
            TableName = PoolTable.name,
            Key = rootKey poolId,
            UpdateExpression = $"SET {name PoolTable.Attribute.activeOperation} = :active",
            ConditionExpression = checkpointOwnerCondition,
            ExpressionAttributeNames = Dictionary(dict checkpointNames),
            ExpressionAttributeValues =
                values
                    [ ":active", activeOperationAttribute next
                      ":operationId", s next.OperationId
                      ":ownerToken", s expectedOwnerToken ]
        )

    /// Completes the receipt and clears the checkpoint, fenced by the owner token. A trend
    /// kickoff, when given, becomes the root's in the same transaction.
    let complete
        (poolId: Ulid)
        (active: ActiveOperation)
        (unresolvedDetails: int)
        (trendKickoff: DateTime option)
        (now: DateTime)
        =
        let completeReceipt =
            Update(
                TableName = PoolTable.name,
                Key = receiptKey poolId active.OperationId,
                UpdateExpression =
                    $"SET {name PoolTable.Attribute.operationState} = :completed, \
                     {name PoolTable.Attribute.completedDateTime} = :now, \
                     {name PoolTable.Attribute.unresolvedDetails} = :unresolved",
                ConditionExpression =
                    $"{name PoolTable.Attribute.operationState} = :admitted AND {name PoolTable.Attribute.operationRevision} = :revision",
                ExpressionAttributeNames =
                    names
                        [ PoolTable.Attribute.operationState
                          PoolTable.Attribute.completedDateTime
                          PoolTable.Attribute.unresolvedDetails
                          PoolTable.Attribute.operationRevision ],
                ExpressionAttributeValues =
                    values
                        [ ":completed", s (receiptStateValue Completed)
                          ":admitted", s (receiptStateValue Admitted)
                          ":now", s (instant now)
                          ":unresolved", ni unresolvedDetails
                          ":revision", n active.Revision ]
            )

        let trendAssignment, trendNames, trendValues =
            match trendKickoff with
            | Some kickoff ->
                $", {name PoolTable.Attribute.trendKickoffDateTime} = :trendKickoff",
                [ name PoolTable.Attribute.trendKickoffDateTime, PoolTable.Attribute.trendKickoffDateTime ],
                [ ":trendKickoff", s (instant kickoff) ]
            | None -> "", [], []

        let clearCheckpoint =
            Update(
                TableName = PoolTable.name,
                Key = rootKey poolId,
                UpdateExpression =
                    $"REMOVE {name PoolTable.Attribute.activeOperation} SET {name PoolTable.Attribute.lastCompletedOperationRevision} = :revision{trendAssignment}",
                ConditionExpression = checkpointOwnerCondition,
                ExpressionAttributeNames =
                    Dictionary(
                        dict (
                            checkpointNames
                            @ [ name PoolTable.Attribute.lastCompletedOperationRevision,
                                PoolTable.Attribute.lastCompletedOperationRevision ]
                            @ trendNames
                        )
                    ),
                ExpressionAttributeValues =
                    values (
                        [ ":revision", n active.Revision
                          ":operationId", s active.OperationId
                          ":ownerToken", s active.OwnerToken ]
                        @ trendValues
                    )
            )

        TransactWriteItemsRequest(
            TransactItems =
                List([ TransactWriteItem(Update = completeReceipt); TransactWriteItem(Update = clearCheckpoint) ])
        )

    let membershipPage (poolId: Ulid) (afterGamblerId: string option) (limit: int) =
        QueryRequest(
            TableName = PoolTable.name,
            ConsistentRead = true,
            Limit = limit,
            KeyConditionExpression = $"{name Key.pk} = :pk AND begins_with({name Key.sk}, :prefix)",
            ExpressionAttributeNames = names [ Key.pk; Key.sk ],
            ExpressionAttributeValues =
                values [ ":pk", s (poolKey poolId); ":prefix", s $"{PoolTable.Prefix.gambler}#" ],
            ExclusiveStartKey =
                match afterGamblerId with
                | Some gamblerId ->
                    Dictionary(
                        dict
                            [ Key.pk, s (poolKey poolId)
                              Key.sk, s (KeyPrefix.build PoolTable.Prefix.gambler gamblerId) ]
                    )
                | None -> null
        )

    let detailStates (poolId: Ulid) (matchId: Ulid) (gamblerIds: Ulid list) =
        BatchGetItemRequest(
            RequestItems =
                Dictionary(
                    dict
                        [ PoolTable.name,
                          KeysAndAttributes(
                              ConsistentRead = true,
                              Keys = List(gamblerIds |> List.map (fun gamblerId -> detailKey poolId gamblerId matchId))
                          ) ]
                )
        )

    let poolsWithMatch (matchId: Ulid) (start: Dictionary<string, AttributeValue>) =
        QueryRequest(
            TableName = PoolTable.name,
            IndexName = PoolTable.Index.scoresByMatch,
            KeyConditionExpression = $"{name PoolTable.Attribute.getPoolGamblerScoresByMatchPk} = :pk",
            ProjectionExpression = name PoolTable.Attribute.getPoolGamblerScoresByMatchSk,
            ExpressionAttributeNames =
                names
                    [ PoolTable.Attribute.getPoolGamblerScoresByMatchPk
                      PoolTable.Attribute.getPoolGamblerScoresByMatchSk ],
            ExpressionAttributeValues = values [ ":pk", s (KeyPrefix.build PoolTable.Prefix.match' matchId.Value) ],
            ExclusiveStartKey = start
        )

    let private detailExpectationCondition (change: SettlementChange) =
        match change.Expectation with
        | Unsettled ->
            $"attribute_not_exists({name PoolTable.Attribute.computedRequestId}) AND attribute_not_exists({name PoolTable.Attribute.resultVersion})",
            []
        | SettledAt version -> $"{name PoolTable.Attribute.resultVersion} = :expectedResultVersion", [ ":expectedResultVersion", ni version ]
        | LegacySettled ->
            $"attribute_exists({name PoolTable.Attribute.computedRequestId}) AND attribute_not_exists({name PoolTable.Attribute.resultVersion}) AND {name PoolTable.Attribute.score} = :previousAward",
            [ ":previousAward", ni (change.PreviousAward |> Option.defaultValue 0) ]

    /// The detail award/result and the membership total, guarded by generation, the detail's
    /// expected result version, and the membership's expected score version.
    let settle (write: SettlementWrite) =
        let change = write.Change
        let expectation, expectationValues = detailExpectationCondition change

        let detail =
            Update(
                TableName = PoolTable.name,
                Key = detailKey write.PoolId write.GamblerId write.MatchId,
                UpdateExpression =
                    $"SET {name PoolTable.Attribute.homeTeamScore} = :home, \
                     {name PoolTable.Attribute.awayTeamScore} = :away, \
                     {name PoolTable.Attribute.score} = :award, \
                     {name PoolTable.Attribute.resultVersion} = :resultVersion, \
                     {name PoolTable.Attribute.settlementOperationId} = :operationId, \
                     {name PoolTable.Attribute.settlementOperationRevision} = :operationRevision, \
                     {name PoolTable.Attribute.computedRequestId} = if_not_exists({name PoolTable.Attribute.computedRequestId}, :operationId), \
                     {name PoolTable.Attribute.computedDateTime} = if_not_exists({name PoolTable.Attribute.computedDateTime}, :now), \
                     {name PoolTable.Attribute.computedMatchDateTime} = if_not_exists({name PoolTable.Attribute.computedMatchDateTime}, {name PoolTable.Attribute.matchDateTime})",
                ConditionExpression =
                    $"attribute_exists({name Key.pk}) AND {name PoolTable.Attribute.poolMembershipGeneration} = :generation AND {expectation}",
                ExpressionAttributeNames =
                    names
                        [ Key.pk
                          PoolTable.Attribute.homeTeamScore
                          PoolTable.Attribute.awayTeamScore
                          PoolTable.Attribute.score
                          PoolTable.Attribute.resultVersion
                          PoolTable.Attribute.settlementOperationId
                          PoolTable.Attribute.settlementOperationRevision
                          PoolTable.Attribute.computedRequestId
                          PoolTable.Attribute.computedDateTime
                          PoolTable.Attribute.computedMatchDateTime
                          PoolTable.Attribute.matchDateTime
                          PoolTable.Attribute.poolMembershipGeneration ],
                ExpressionAttributeValues =
                    values (
                        [ ":home", ni write.Result.HomeTeamScore
                          ":away", ni write.Result.AwayTeamScore
                          ":award", ni change.Award
                          ":resultVersion", ni write.Result.Version
                          ":operationId", s write.OperationId
                          ":operationRevision", n write.OperationRevision
                          ":now", s (instant write.ComputedAt)
                          ":generation", s change.Generation ]
                        @ expectationValues
                    ),
                ReturnValuesOnConditionCheckFailure = ReturnValuesOnConditionCheckFailure.ALL_OLD
            )

        let membershipCondition =
            $"{name PoolTable.Attribute.membershipGeneration} = :generation AND \
             {name PoolTable.Attribute.scoreVersion} = :expectedScoreVersion AND \
             {name PoolTable.Attribute.score} = :previousTotal"

        let membershipNames =
            [ PoolTable.Attribute.membershipGeneration
              PoolTable.Attribute.scoreVersion
              PoolTable.Attribute.score ]

        let membershipValues =
            [ ":generation", s change.Generation
              ":expectedScoreVersion", n change.ExpectedScoreVersion
              ":previousTotal", ni change.PreviousTotal ]

        let membershipKey = membershipKey write.PoolId write.GamblerId

        let membership =
            if change.TotalChanged then
                TransactWriteItem(
                    Update =
                        Update(
                            TableName = PoolTable.name,
                            Key = membershipKey,
                            UpdateExpression =
                                $"SET {name PoolTable.Attribute.beforeScore} = {name PoolTable.Attribute.score}, \
                                 {name PoolTable.Attribute.score} = :total, \
                                 {name PoolTable.Attribute.scoreVersion} = :nextScoreVersion",
                            ConditionExpression = membershipCondition,
                            ExpressionAttributeNames = names (PoolTable.Attribute.beforeScore :: membershipNames),
                            ExpressionAttributeValues =
                                values (
                                    membershipValues
                                    @ [ ":total", ni change.Total
                                        ":nextScoreVersion", n (change.ExpectedScoreVersion + 1L) ]
                                )
                        )
                )
            elif change.RefreshesBeforeScore then
                TransactWriteItem(
                    Update =
                        Update(
                            TableName = PoolTable.name,
                            Key = membershipKey,
                            UpdateExpression =
                                $"SET {name PoolTable.Attribute.beforeScore} = {name PoolTable.Attribute.score}",
                            ConditionExpression = membershipCondition,
                            ExpressionAttributeNames = names (PoolTable.Attribute.beforeScore :: membershipNames),
                            ExpressionAttributeValues = values membershipValues
                        )
                )
            else
                TransactWriteItem(
                    ConditionCheck =
                        ConditionCheck(
                            TableName = PoolTable.name,
                            Key = membershipKey,
                            ConditionExpression = membershipCondition,
                            ExpressionAttributeNames = names membershipNames,
                            ExpressionAttributeValues = values membershipValues
                        )
                )

        TransactWriteItemsRequest(TransactItems = List([ TransactWriteItem(Update = detail); membership ]))

    let private membershipPosition (write: PositionWrite) =
        let change = write.Change

        let markerCondition =
            match write.ExpectedPositionOperationRevision with
            | Some _ -> $"{name PoolTable.Attribute.positionOperationRevision} = :expectedPositionRevision"
            | None -> $"attribute_not_exists({name PoolTable.Attribute.positionOperationRevision})"

        let calculationCondition =
            $"(attribute_not_exists({name PoolTable.Attribute.positionCalculationRevision}) OR {name PoolTable.Attribute.positionCalculationRevision} <= :calculationRevision)"

        let assignments =
            [ $"{name PoolTable.Attribute.position} = :position"
              $"{name PoolTable.Attribute.positionCalculationRevision} = :calculationRevision"
              if change.FirstWrite then
                  $"{name PoolTable.Attribute.positionOperationRevision} = :operationRevision"
              if change.MembershipBeforePosition.IsSome then
                  $"{name PoolTable.Attribute.beforePosition} = :beforePosition" ]

        Update(
            TableName = PoolTable.name,
            Key = membershipKey write.PoolId write.GamblerId,
            UpdateExpression = "SET " + String.Join(", ", assignments),
            ConditionExpression =
                $"{name PoolTable.Attribute.membershipGeneration} = :generation AND \
                 {name PoolTable.Attribute.scoreVersion} = :scoreVersion AND {markerCondition} AND {calculationCondition}",
            ExpressionAttributeNames =
                names (
                    [ PoolTable.Attribute.position
                      PoolTable.Attribute.positionCalculationRevision
                      PoolTable.Attribute.positionOperationRevision
                      PoolTable.Attribute.membershipGeneration
                      PoolTable.Attribute.scoreVersion ]
                    @ (if change.MembershipBeforePosition.IsSome then [ PoolTable.Attribute.beforePosition ] else [])
                ),
            ExpressionAttributeValues =
                values (
                    [ ":position", ni change.Position
                      ":calculationRevision", ni write.CalculationRevision
                      ":generation", s write.Generation
                      ":scoreVersion", n write.ExpectedScoreVersion ]
                    @ (if change.FirstWrite then [ ":operationRevision", n write.OperationRevision ] else [])
                    @ (change.MembershipBeforePosition
                       |> Option.map (fun before -> ":beforePosition", ni before)
                       |> Option.toList)
                    @ (write.ExpectedPositionOperationRevision
                       |> Option.map (fun revision -> ":expectedPositionRevision", n revision)
                       |> Option.toList)
                )
        )

    let private detailPosition (write: PositionWrite) (matchId: Ulid) =
        let change = write.Change

        let assignments =
            [ $"{name PoolTable.Attribute.position} = :position"
              $"{name PoolTable.Attribute.positionCalculationRevision} = :calculationRevision"
              if change.FirstWrite then
                  $"{name PoolTable.Attribute.positionOperationRevision} = :operationRevision"

                  if change.DetailBeforePosition.IsSome then
                      $"{name PoolTable.Attribute.beforePosition} = :beforePosition" ]

        Update(
            TableName = PoolTable.name,
            Key = detailKey write.PoolId write.GamblerId matchId,
            UpdateExpression = "SET " + String.Join(", ", assignments),
            ConditionExpression =
                $"attribute_exists({name Key.pk}) AND {name PoolTable.Attribute.poolMembershipGeneration} = :generation AND \
                 (attribute_not_exists({name PoolTable.Attribute.positionOperationRevision}) OR {name PoolTable.Attribute.positionOperationRevision} <= :operationRevision)",
            ExpressionAttributeNames =
                names (
                    [ Key.pk
                      PoolTable.Attribute.position
                      PoolTable.Attribute.positionCalculationRevision
                      PoolTable.Attribute.positionOperationRevision
                      PoolTable.Attribute.poolMembershipGeneration ]
                    @ (if change.FirstWrite && change.DetailBeforePosition.IsSome then
                           [ PoolTable.Attribute.beforePosition ]
                       else
                           [])
                ),
            ExpressionAttributeValues =
                values (
                    [ ":position", ni change.Position
                      ":calculationRevision", ni write.CalculationRevision
                      ":operationRevision", n write.OperationRevision
                      ":generation", s write.Generation ]
                    @ (match change.FirstWrite, change.DetailBeforePosition with
                       | true, Some before -> [ ":beforePosition", ni before ]
                       | _ -> [])
                )
        )

    /// The membership position alone, or the membership and target match detail positions
    /// in one transaction.
    let position (write: PositionWrite) =
        let items =
            [ TransactWriteItem(Update = membershipPosition write) ]
            @ (write.TargetMatchId
               |> Option.map (fun matchId -> TransactWriteItem(Update = detailPosition write matchId))
               |> Option.toList)

        TransactWriteItemsRequest(TransactItems = List(items))
