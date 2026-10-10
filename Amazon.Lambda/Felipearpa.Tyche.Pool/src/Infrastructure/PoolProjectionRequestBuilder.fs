namespace Felipearpa.Tyche.Pool.Infrastructure

open System
open System.Collections.Generic
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure.PoolSettlementDictionaryTransformer
open Felipearpa.Tyche.Pool.Infrastructure.PoolSettlementRequestBuilder
open Felipearpa.Type

/// Requests for membership/detail identity and the guarded pool-total projection.
module PoolProjectionRequestBuilder =

    let private stateName (total: TotalProjection) =
        match total with
        | KnownTotal _ -> "KNOWN"
        | UnavailableTotal -> "UNAVAILABLE"

    /// The projection attributes for a detail bound to the membership, including both
    /// ranking index keys.
    let projectionAttributes (poolId: Ulid) (gamblerId: Ulid) (matchId: Ulid) (binding: MembershipBinding) =
        [ PoolTable.Attribute.poolMembershipGeneration, s binding.Generation
          PoolTable.Attribute.poolScoreVersion, n binding.ScoreVersion
          PoolTable.Attribute.totalPoolPointsState, s (stateName binding.Total)
          PoolTable.Attribute.rankingActive, AttributeValue(BOOL = true)
          PoolTable.Attribute.getPoolMatchBetsByTotalPk, s (RankingKey.partition poolId matchId)
          PoolTable.Attribute.getPoolMatchBetsByTotalSk, s (RankingKey.sortKey binding.Total gamblerId) ]
        @ (match binding.Total with
           | KnownTotal total -> [ PoolTable.Attribute.totalPoolPoints, ni total ]
           | UnavailableTotal -> [])

    let private membershipBindingCheck (poolId: Ulid) (gamblerId: Ulid) (binding: MembershipBinding) =
        ConditionCheck(
            TableName = PoolTable.name,
            Key = membershipKey poolId gamblerId,
            ConditionExpression =
                $"{name PoolTable.Attribute.membershipGeneration} = :generation AND {name PoolTable.Attribute.scoreVersion} = :scoreVersion",
            ExpressionAttributeNames = names [ PoolTable.Attribute.membershipGeneration; PoolTable.Attribute.scoreVersion ],
            ExpressionAttributeValues =
                values [ ":generation", s binding.Generation; ":scoreVersion", n binding.ScoreVersion ]
        )

    let betProjectionPage (poolId: Ulid) (gamblerId: Ulid) (afterMatchId: string option) (limit: int) =
        QueryRequest(
            TableName = PoolTable.name,
            ConsistentRead = true,
            Limit = limit,
            KeyConditionExpression = $"{name Key.pk} = :pk",
            ExpressionAttributeNames = names [ Key.pk ],
            ExpressionAttributeValues = values [ ":pk", s (detailPartition poolId gamblerId) ],
            ExclusiveStartKey =
                match afterMatchId with
                | Some matchId ->
                    Dictionary(
                        dict
                            [ Key.pk, s (detailPartition poolId gamblerId)
                              Key.sk, s (KeyPrefix.build PoolTable.Prefix.match' matchId) ]
                    )
                | None -> null
        )

    let initializeMembershipIdentity (poolId: Ulid) (gamblerId: Ulid) (generation: string) =
        UpdateItemRequest(
            TableName = PoolTable.name,
            Key = membershipKey poolId gamblerId,
            UpdateExpression =
                $"SET {name PoolTable.Attribute.membershipGeneration} = :generation, \
                 {name PoolTable.Attribute.scoreVersion} = if_not_exists({name PoolTable.Attribute.scoreVersion}, :initialScoreVersion)",
            ConditionExpression =
                $"attribute_exists({name Key.pk}) AND attribute_not_exists({name PoolTable.Attribute.membershipGeneration})",
            ExpressionAttributeNames =
                names [ Key.pk; PoolTable.Attribute.membershipGeneration; PoolTable.Attribute.scoreVersion ],
            ExpressionAttributeValues = values [ ":generation", s generation; ":initialScoreVersion", n 1L ]
        )

    let bindDetailIdentity (poolId: Ulid) (gamblerId: Ulid) (matchId: Ulid) (generation: string) (isComputed: bool) =
        let detail =
            if isComputed then
                Update(
                    TableName = PoolTable.name,
                    Key = detailKey poolId gamblerId matchId,
                    UpdateExpression =
                        $"SET {name PoolTable.Attribute.poolMembershipGeneration} = :generation, \
                         {name PoolTable.Attribute.resultVersion} = if_not_exists({name PoolTable.Attribute.resultVersion}, :legacyResultVersion)",
                    ConditionExpression =
                        $"attribute_exists({name Key.pk}) AND attribute_not_exists({name PoolTable.Attribute.poolMembershipGeneration}) AND attribute_exists({name PoolTable.Attribute.computedRequestId})",
                    ExpressionAttributeNames =
                        names
                            [ Key.pk
                              PoolTable.Attribute.poolMembershipGeneration
                              PoolTable.Attribute.resultVersion
                              PoolTable.Attribute.computedRequestId ],
                    ExpressionAttributeValues = values [ ":generation", s generation; ":legacyResultVersion", n 1L ]
                )
            else
                Update(
                    TableName = PoolTable.name,
                    Key = detailKey poolId gamblerId matchId,
                    UpdateExpression = $"SET {name PoolTable.Attribute.poolMembershipGeneration} = :generation",
                    ConditionExpression =
                        $"attribute_exists({name Key.pk}) AND attribute_not_exists({name PoolTable.Attribute.poolMembershipGeneration}) AND attribute_not_exists({name PoolTable.Attribute.computedRequestId})",
                    ExpressionAttributeNames =
                        names
                            [ Key.pk
                              PoolTable.Attribute.poolMembershipGeneration
                              PoolTable.Attribute.computedRequestId ],
                    ExpressionAttributeValues = values [ ":generation", s generation ]
                )

        let membership =
            ConditionCheck(
                TableName = PoolTable.name,
                Key = membershipKey poolId gamblerId,
                ConditionExpression = $"{name PoolTable.Attribute.membershipGeneration} = :generation",
                ExpressionAttributeNames = names [ PoolTable.Attribute.membershipGeneration ],
                ExpressionAttributeValues = values [ ":generation", s generation ]
            )

        TransactWriteItemsRequest(
            TransactItems = List([ TransactWriteItem(Update = detail); TransactWriteItem(ConditionCheck = membership) ])
        )

    let private assignments (attributes: (string * AttributeValue) list) =
        attributes
        |> List.map (fun (attribute, _) -> $"{name attribute} = :{attribute}")
        |> fun parts -> String.Join(", ", parts)

    let private attributeValues (attributes: (string * AttributeValue) list) =
        attributes |> List.map (fun (attribute, value) -> $":{attribute}", value)

    let applyTotal (poolId: Ulid) (gamblerId: Ulid) (matchId: Ulid) (binding: MembershipBinding) =
        let attributes =
            [ PoolTable.Attribute.poolScoreVersion, n binding.ScoreVersion
              PoolTable.Attribute.totalPoolPointsState, s (stateName binding.Total)
              PoolTable.Attribute.getPoolMatchBetsByTotalSk, s (RankingKey.sortKey binding.Total gamblerId) ]
            @ (match binding.Total with
               | KnownTotal total -> [ PoolTable.Attribute.totalPoolPoints, ni total ]
               | UnavailableTotal -> [])

        let removal =
            match binding.Total with
            | KnownTotal _ -> ""
            | UnavailableTotal -> $" REMOVE {name PoolTable.Attribute.totalPoolPoints}"

        UpdateItemRequest(
            TableName = PoolTable.name,
            Key = detailKey poolId gamblerId matchId,
            UpdateExpression = $"SET {assignments attributes}{removal}",
            ConditionExpression =
                $"{name PoolTable.Attribute.poolMembershipGeneration} = :generation AND \
                 {name PoolTable.Attribute.rankingActive} = :active AND \
                 {name PoolTable.Attribute.poolScoreVersion} < :{PoolTable.Attribute.poolScoreVersion}",
            ExpressionAttributeNames =
                names (
                    (attributes |> List.map fst)
                    @ [ PoolTable.Attribute.poolMembershipGeneration
                        PoolTable.Attribute.rankingActive
                        PoolTable.Attribute.totalPoolPoints ]
                    |> List.distinct
                ),
            ExpressionAttributeValues =
                values (
                    attributeValues attributes
                    @ [ ":generation", s binding.Generation; ":active", AttributeValue(BOOL = true) ]
                ),
            ReturnValuesOnConditionCheckFailure = ReturnValuesOnConditionCheckFailure.ALL_OLD
        )

    let initializeProjection (poolId: Ulid) (gamblerId: Ulid) (matchId: Ulid) (binding: MembershipBinding) =
        let attributes =
            projectionAttributes poolId gamblerId matchId binding
            |> List.filter (fun (attribute, _) -> attribute <> PoolTable.Attribute.poolMembershipGeneration)

        let removal =
            match binding.Total with
            | KnownTotal _ -> ""
            | UnavailableTotal -> $" REMOVE {name PoolTable.Attribute.totalPoolPoints}"

        let detail =
            Update(
                TableName = PoolTable.name,
                Key = detailKey poolId gamblerId matchId,
                UpdateExpression = $"SET {assignments attributes}{removal}",
                ConditionExpression =
                    $"attribute_exists({name Key.pk}) AND {name PoolTable.Attribute.poolMembershipGeneration} = :generation AND \
                     (attribute_not_exists({name PoolTable.Attribute.poolScoreVersion}) OR \
                      {name PoolTable.Attribute.poolScoreVersion} < :{PoolTable.Attribute.poolScoreVersion} OR \
                      attribute_not_exists({name PoolTable.Attribute.getPoolMatchBetsByTotalSk}))",
                ExpressionAttributeNames =
                    names (
                        (attributes |> List.map fst)
                        @ [ Key.pk
                            PoolTable.Attribute.poolMembershipGeneration
                            PoolTable.Attribute.totalPoolPoints ]
                        |> List.distinct
                    ),
                ExpressionAttributeValues = values (attributeValues attributes @ [ ":generation", s binding.Generation ])
            )

        TransactWriteItemsRequest(
            TransactItems =
                List(
                    [ TransactWriteItem(Update = detail)
                      TransactWriteItem(ConditionCheck = membershipBindingCheck poolId gamblerId binding) ]
                )
        )

    let deactivateRanking (poolId: Ulid) (gamblerId: Ulid) (matchId: Ulid) (generation: string) =
        UpdateItemRequest(
            TableName = PoolTable.name,
            Key = detailKey poolId gamblerId matchId,
            UpdateExpression =
                $"SET {name PoolTable.Attribute.rankingActive} = :inactive \
                 REMOVE {name PoolTable.Attribute.getPoolMatchBetsByTotalPk}, {name PoolTable.Attribute.getPoolMatchBetsByTotalSk}",
            ConditionExpression = $"{name PoolTable.Attribute.poolMembershipGeneration} = :generation",
            ExpressionAttributeNames =
                names
                    [ PoolTable.Attribute.rankingActive
                      PoolTable.Attribute.getPoolMatchBetsByTotalPk
                      PoolTable.Attribute.getPoolMatchBetsByTotalSk
                      PoolTable.Attribute.poolMembershipGeneration ],
            ExpressionAttributeValues = values [ ":inactive", AttributeValue(BOOL = false); ":generation", s generation ]
        )

    let deleteUncomputed (poolId: Ulid) (gamblerId: Ulid) (matchId: Ulid) (generation: string option) =
        let generationCondition, generationValues =
            match generation with
            | Some value -> $"{name PoolTable.Attribute.poolMembershipGeneration} = :generation", [ ":generation", s value ]
            | None -> $"attribute_not_exists({name PoolTable.Attribute.poolMembershipGeneration})", []

        DeleteItemRequest(
            TableName = PoolTable.name,
            Key = detailKey poolId gamblerId matchId,
            ConditionExpression =
                $"attribute_not_exists({name PoolTable.Attribute.computedRequestId}) AND {generationCondition}",
            ExpressionAttributeNames =
                names [ PoolTable.Attribute.computedRequestId; PoolTable.Attribute.poolMembershipGeneration ],
            ExpressionAttributeValues = (if generationValues.IsEmpty then null else values generationValues)
        )

    let setReadiness (poolId: Ulid) (report: RankingReadinessReport) (observedMembershipRevision: int64) (now: DateTime) =
        let readiness =
            AttributeValue(
                M =
                    Dictionary(
                        dict
                            [ "state", s (match report.Readiness with | Ready -> "READY" | NotReady -> "NOT_READY")
                              "schemaVersion", ni RankingKey.schemaVersion
                              "checkedMembers", ni report.CheckedMembers
                              "checkedDetails", ni report.CheckedDetails
                              "unresolved", ni report.Unresolved
                              "checkedAt", s (instant now) ]
                    )
            )

        let revisionCondition =
            if observedMembershipRevision = 0L then
                $"(attribute_not_exists({name PoolTable.Attribute.membershipRevision}) OR {name PoolTable.Attribute.membershipRevision} = :revision)"
            else
                $"{name PoolTable.Attribute.membershipRevision} = :revision"

        UpdateItemRequest(
            TableName = PoolTable.name,
            Key = rootKey poolId,
            UpdateExpression = $"SET {name PoolTable.Attribute.rankingReadiness} = :readiness",
            ConditionExpression = $"attribute_exists({name Key.pk}) AND {revisionCondition}",
            ExpressionAttributeNames =
                names [ Key.pk; PoolTable.Attribute.rankingReadiness; PoolTable.Attribute.membershipRevision ],
            ExpressionAttributeValues = values [ ":readiness", readiness; ":revision", n observedMembershipRevision ]
        )

    /// Creates a detail in one transaction with a check that the membership still has the
    /// binding's generation and score version. Without a binding, the detail is created
    /// unbound and the check requires a membership whose identity is not initialized yet, so
    /// identity initialization binds it later.
    let materialize
        (item: IDictionary<string, AttributeValue>)
        (poolId: Ulid)
        (gamblerId: Ulid)
        (binding: MembershipBinding option)
        =
        let put =
            Put(
                TableName = PoolTable.name,
                Item = Dictionary(item),
                ConditionExpression = $"attribute_not_exists({name Key.pk})",
                ExpressionAttributeNames = names [ Key.pk ]
            )

        let membershipCheck =
            match binding with
            | Some binding -> membershipBindingCheck poolId gamblerId binding
            | None ->
                ConditionCheck(
                    TableName = PoolTable.name,
                    Key = membershipKey poolId gamblerId,
                    ConditionExpression =
                        $"attribute_exists({name Key.pk}) AND attribute_not_exists({name PoolTable.Attribute.membershipGeneration})",
                    ExpressionAttributeNames = names [ Key.pk; PoolTable.Attribute.membershipGeneration ]
                )

        TransactWriteItemsRequest(
            TransactItems = List([ TransactWriteItem(Put = put); TransactWriteItem(ConditionCheck = membershipCheck) ])
        )
