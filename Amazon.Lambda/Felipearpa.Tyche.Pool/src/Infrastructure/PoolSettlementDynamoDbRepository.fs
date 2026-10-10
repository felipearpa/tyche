namespace Felipearpa.Tyche.Pool.Infrastructure

#nowarn "3536"

open System
open System.Collections.Generic
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure.PoolSettlementDictionaryTransformer
open Felipearpa.Type

/// DynamoDB access for ordered pool operations and the pool-total projection.
type PoolSettlementDynamoDbRepository(client: IAmazonDynamoDB) =

    [<Literal>]
    let batchGetLimit = 100

    [<Literal>]
    let unprocessedKeyAttempts = 8

    let send (request: Async<'T>) = request |> Async.Ignore

    let tryGetItem (request: GetItemRequest) =
        async {
            let! response = client.GetItemAsync request |> Async.AwaitTask

            return
                if response.IsItemSet && not (isNull response.Item) && response.Item.Count > 0 then
                    Some response.Item
                else
                    None
        }

    let lastSortKeySuffix (prefix: string) (items: List<Dictionary<string, AttributeValue>>) (lastEvaluatedKey: Dictionary<string, AttributeValue>) =
        if isNull lastEvaluatedKey || lastEvaluatedKey.Count = 0 || items.Count = 0 then
            None
        else
            let last = items[items.Count - 1]
            Some(last["sk"].S.Substring(prefix.Length + 1))

    let batchGetAll (request: BatchGetItemRequest) =
        let rec loop (pending: BatchGetItemRequest) attempt (acc: Dictionary<string, AttributeValue> list) =
            async {
                let! response = client.BatchGetItemAsync pending |> Async.AwaitTask

                let found =
                    match response.Responses |> Option.ofObj with
                    | Some responses ->
                        match responses.TryGetValue PoolTable.name with
                        | true, items -> items |> List.ofSeq
                        | _ -> []
                    | None -> []

                let acc = acc @ found

                match response.UnprocessedKeys |> Option.ofObj with
                | Some unprocessed when unprocessed.Count > 0 ->
                    if attempt >= unprocessedKeyAttempts then
                        return
                            raise (
                                ProvisionedThroughputExceededException
                                    $"BatchGetItem left keys unprocessed after {unprocessedKeyAttempts} attempts"
                            )
                    else
                        let delay = 25 * (1 <<< attempt)
                        do! Async.Sleep(Random.Shared.Next(delay / 2, delay + 1))
                        return! loop (BatchGetItemRequest(RequestItems = unprocessed)) (attempt + 1) acc
                | _ -> return acc
            }

        loop request 0 []

    let membershipPage poolId afterGamblerId limit =
        async {
            let! response =
                client.QueryAsync(PoolSettlementRequestBuilder.membershipPage poolId afterGamblerId limit)
                |> Async.AwaitTask

            let next = lastSortKeySuffix PoolTable.Prefix.gambler response.Items response.LastEvaluatedKey
            return response.Items |> Seq.map toMembershipState |> List.ofSeq, next
        }

    let membership poolId gamblerId =
        async {
            let! item = tryGetItem (PoolSettlementRequestBuilder.getMembership poolId gamblerId)
            return item |> Option.map toMembershipState
        }

    interface IPoolSettlementRepository with

        member _.GetRootAsync(poolId) =
            async {
                let! item = tryGetItem (PoolSettlementRequestBuilder.getRoot poolId)
                return item |> Option.map toSettlementRoot
            }

        member _.GetReceiptAsync(poolId, operationId) =
            async {
                let! item = tryGetItem (PoolSettlementRequestBuilder.getReceipt poolId operationId)
                return item |> Option.map toReceipt
            }

        member _.AdmitAsync(root, receipt, active) =
            PoolSettlementRequestBuilder.admit root receipt active DateTime.UtcNow
            |> client.TransactWriteItemsAsync
            |> Async.AwaitTask
            |> send
            |> GuardedWrite.run

        member _.RecordCompletedAsync(poolId, receipt) =
            async {
                let! recorded =
                    PoolSettlementRequestBuilder.recordReceipt poolId receipt DateTime.UtcNow true
                    |> client.PutItemAsync
                    |> Async.AwaitTask
                    |> send
                    |> GuardedWrite.succeeded

                if not recorded then
                    let! existing = tryGetItem (PoolSettlementRequestBuilder.getReceipt poolId receipt.OperationId)

                    match existing |> Option.map toReceipt with
                    | Some found when found.State = Completed && found.Fingerprint = receipt.Fingerprint -> ()
                    | other ->
                        return
                            raise (
                                InvalidOperationException
                                    $"Receipt {receipt.OperationId} could not be completed; stored state {other}"
                            )
            }

        member _.RecordDeferredAsync(poolId, receipt) =
            async {
                let! _ =
                    PoolSettlementRequestBuilder.recordReceipt poolId receipt DateTime.UtcNow false
                    |> client.PutItemAsync
                    |> Async.AwaitTask
                    |> send
                    |> GuardedWrite.succeeded

                return ()
            }

        member _.GetDeferredReceiptsAsync(poolId) =
            let rec loop (start: Dictionary<string, AttributeValue>) acc =
                async {
                    let! response =
                        client.QueryAsync(PoolSettlementRequestBuilder.getDeferredReceipts poolId start)
                        |> Async.AwaitTask

                    let acc = acc @ (response.Items |> Seq.map toReceipt |> List.ofSeq)

                    match response.LastEvaluatedKey |> Option.ofObj with
                    | Some key when key.Count > 0 -> return! loop key acc
                    | _ -> return acc
                }

            async {
                let! receipts = loop null []
                return receipts |> List.sortBy (fun receipt -> receipt.DeferredAt, receipt.OperationId)
            }

        member _.ReplaceCheckpointAsync(poolId, expectedOwnerToken, next) =
            PoolSettlementRequestBuilder.replaceCheckpoint poolId expectedOwnerToken next
            |> client.UpdateItemAsync
            |> Async.AwaitTask
            |> send
            |> GuardedWrite.run

        member _.CompleteAsync(poolId, active, unresolvedDetails) =
            PoolSettlementRequestBuilder.complete poolId active unresolvedDetails DateTime.UtcNow
            |> client.TransactWriteItemsAsync
            |> Async.AwaitTask
            |> send
            |> GuardedWrite.run

        member _.GetMembershipPageAsync(poolId, afterGamblerId, limit) = membershipPage poolId afterGamblerId limit

        member _.GetMembershipAsync(poolId, gamblerId) = membership poolId gamblerId

        member _.GetBetSettlementStatesAsync(poolId, matchId, gamblerIds) =
            async {
                let! chunks =
                    gamblerIds
                    |> List.chunkBySize batchGetLimit
                    |> List.map (fun chunk -> batchGetAll (PoolSettlementRequestBuilder.detailStates poolId matchId chunk))
                    |> Async.Sequential

                return chunks |> List.concat |> List.map toBetSettlementState
            }

        member _.GetBetSettlementStateAsync(poolId, gamblerId, matchId) =
            async {
                let! item = tryGetItem (PoolSettlementRequestBuilder.getDetail poolId gamblerId matchId)
                return item |> Option.map toBetSettlementState
            }

        member _.ApplySettlementAsync(write) =
            PoolSettlementRequestBuilder.settle write
            |> client.TransactWriteItemsAsync
            |> Async.AwaitTask
            |> send
            |> GuardedWrite.run

        member _.ApplyPositionAsync(write) =
            PoolSettlementRequestBuilder.position write
            |> client.TransactWriteItemsAsync
            |> Async.AwaitTask
            |> send
            |> GuardedWrite.run

        member _.GetRetainedBetPageAsync(poolId, gamblerId, afterMatchId, limit) =
            async {
                let! response =
                    client.QueryAsync(PoolProjectionRequestBuilder.betProjectionPage poolId gamblerId afterMatchId limit)
                    |> Async.AwaitTask

                let next = lastSortKeySuffix PoolTable.Prefix.match' response.Items response.LastEvaluatedKey
                return response.Items |> Seq.map toRetainedBetState |> List.ofSeq, next
            }

        member _.GetRetainedBetAsync(poolId, gamblerId, matchId) =
            async {
                let! item = tryGetItem (PoolSettlementRequestBuilder.getDetail poolId gamblerId matchId)
                return item |> Option.map toRetainedBetState
            }

        member _.ApplyRebindingAsync(write) =
            PoolProjectionRequestBuilder.rebind write
            |> client.TransactWriteItemsAsync
            |> Async.AwaitTask
            |> send
            |> GuardedWrite.run

        member _.HoldRetainedBetAsync(hold) =
            PoolProjectionRequestBuilder.holdForRestore hold
            |> client.UpdateItemAsync
            |> Async.AwaitTask
            |> send
            |> GuardedWrite.run

        member _.GetPoolsWithMatchPageAsync(matchId, next) =
            async {
                let start =
                    match next with
                    | Some cursor ->
                        StartKeyCodec.tryDecode cursor
                        |> Option.defaultWith (fun () -> raise (ArgumentException "Malformed discovery cursor"))
                    | None -> null

                let! response =
                    client.QueryAsync(PoolSettlementRequestBuilder.poolsWithMatch matchId start)
                    |> Async.AwaitTask

                let poolPrefix = $"{PoolTable.Prefix.pool}#"

                let poolIds =
                    response.Items
                    |> Seq.map (fun item ->
                        let sortKey = item[PoolTable.Attribute.getPoolGamblerScoresByMatchSk].S
                        sortKey.Substring(poolPrefix.Length).Split('#')[0] |> Ulid.newOf)
                    |> Seq.distinct
                    |> List.ofSeq

                let cursor =
                    match response.LastEvaluatedKey |> Option.ofObj with
                    | Some key when key.Count > 0 -> Some(StartKeyCodec.encode key)
                    | _ -> None

                return poolIds, cursor
            }

    interface IPoolProjectionRepository with

        member _.GetMembershipAsync(poolId, gamblerId) = membership poolId gamblerId

        member _.GetMembershipPageAsync(poolId, afterGamblerId, limit) = membershipPage poolId afterGamblerId limit

        member _.GetBetProjectionPageAsync(poolId, gamblerId, afterMatchId, limit) =
            async {
                let! response =
                    client.QueryAsync(PoolProjectionRequestBuilder.betProjectionPage poolId gamblerId afterMatchId limit)
                    |> Async.AwaitTask

                let next = lastSortKeySuffix PoolTable.Prefix.match' response.Items response.LastEvaluatedKey
                return response.Items |> Seq.map toBetProjectionState |> List.ofSeq, next
            }

        member _.InitializeMembershipIdentityAsync(poolId, gamblerId, generation) =
            PoolProjectionRequestBuilder.initializeMembershipIdentity poolId gamblerId generation
            |> client.UpdateItemAsync
            |> Async.AwaitTask
            |> send
            |> GuardedWrite.run

        member _.BindDetailIdentityAsync(poolId, gamblerId, matchId, generation, isComputed) =
            PoolProjectionRequestBuilder.bindDetailIdentity poolId gamblerId matchId generation isComputed
            |> client.TransactWriteItemsAsync
            |> Async.AwaitTask
            |> send
            |> GuardedWrite.run

        member _.ApplyTotalAsync(poolId, gamblerId, matchId, binding) =
            PoolProjectionRequestBuilder.applyTotal poolId gamblerId matchId binding
            |> client.UpdateItemAsync
            |> Async.AwaitTask
            |> send
            |> GuardedWrite.run

        member _.InitializeProjectionAsync(poolId, gamblerId, matchId, binding) =
            PoolProjectionRequestBuilder.initializeProjection poolId gamblerId matchId binding
            |> client.TransactWriteItemsAsync
            |> Async.AwaitTask
            |> send
            |> GuardedWrite.run

        member _.DeactivateRankingAsync(poolId, gamblerId, matchId, generation) =
            PoolProjectionRequestBuilder.deactivateRanking poolId gamblerId matchId generation
            |> client.UpdateItemAsync
            |> Async.AwaitTask
            |> send
            |> GuardedWrite.run

        member _.DeleteUncomputedAsync(poolId, gamblerId, matchId, generation) =
            PoolProjectionRequestBuilder.deleteUncomputed poolId gamblerId matchId generation
            |> client.DeleteItemAsync
            |> Async.AwaitTask
            |> send
            |> GuardedWrite.run

        member _.SetReadinessAsync(poolId, report, observedMembershipRevision) =
            PoolProjectionRequestBuilder.setReadiness poolId report observedMembershipRevision DateTime.UtcNow
            |> client.UpdateItemAsync
            |> Async.AwaitTask
            |> send
            |> GuardedWrite.succeeded
