namespace Felipearpa.Tyche.Pool.Test.Fakes

open System
open System.Collections.Generic
open System.Threading
open System.Threading.Tasks
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Moq
open Felipearpa.Tyche.Pool.Test.Fakes.FakeExpression

type FakeIndex =
    { Name: string
      Hash: string
      Range: string option
      /// None projects every attribute; Some lists the non-key attributes copied into the index.
      Projection: string list option }

type FakeTable =
    { Name: string
      Hash: string
      Range: string option
      Indexes: FakeIndex list }

/// A committed item change, as a NEW_AND_OLD_IMAGES stream would report it.
type FakeChange =
    { Sequence: int64
      TableName: string
      OldImage: Dictionary<string, AttributeValue> option
      NewImage: Dictionary<string, AttributeValue> option }

/// What the fake does with an intercepted request.
type FakeFault =
    | Proceed
    /// Fails before any state changes.
    | FailBefore of exn
    /// Commits the change, then fails as if the response were lost.
    | FailAfterCommit of exn

/// A stateful, thread-safe in-memory DynamoDB used by failure-path tests. It applies
/// conditional writes and transactions atomically, can delay index and eventually
/// consistent reads behind a frozen snapshot, and lets tests inject faults per request.
type FakeDynamoDb(tables: FakeTable list) =
    let gate = obj ()

    let store =
        Dictionary<string, Dictionary<string, Dictionary<string, AttributeValue>>>(
            tables |> Seq.map (fun table -> KeyValuePair(table.Name, Dictionary()))
        )

    let mutable laggedView: Dictionary<string, Dictionary<string, Dictionary<string, AttributeValue>>> option =
        None

    let mutable interceptor: obj -> FakeFault = fun _ -> Proceed
    let mutable unprocessedBatchGetKeys: int = 0
    let calls = Dictionary<string, int>()
    let mutable changes: FakeChange list = []
    let mutable sequence = 0L
    let mutable queryPageSize: int option = None
    let mutable emptyFinalKey = false
    let mutable queries: QueryRequest list = []

    /// Stores or removes an item through client writes, recording the change like a stream.
    let commit (table: FakeTable) (key: string) (next: Dictionary<string, AttributeValue> option) =
        let previous =
            match store[table.Name].TryGetValue key with
            | true, item -> Some(cloneItem item)
            | _ -> None

        match next with
        | Some item -> store[table.Name][key] <- cloneItem item
        | None -> store[table.Name].Remove key |> ignore

        let changed =
            match previous, next with
            | None, None -> false
            | Some before, Some after ->
                before.Count <> after.Count
                || before
                   |> Seq.exists (fun pair ->
                       match after.TryGetValue pair.Key with
                       | true, value -> not (valueEquals value pair.Value)
                       | _ -> true)
            | _ -> true

        if changed then
            sequence <- sequence + 1L

            changes <-
                changes
                @ [ { Sequence = sequence
                      TableName = table.Name
                      OldImage = previous
                      NewImage = next |> Option.map cloneItem } ]

    let tableOf (name: string) =
        tables
        |> List.tryFind (fun table -> table.Name = name)
        |> Option.defaultWith (fun () -> raise (ResourceNotFoundException $"Requested resource not found: {name}"))

    let keyString (table: FakeTable) (item: IDictionary<string, AttributeValue>) =
        let part name =
            match item.TryGetValue name with
            | true, value -> (if isNull value.S then value.N else value.S)
            | _ -> raise (AmazonDynamoDBException $"Missing key attribute {name}")

        match table.Range with
        | Some range -> $"{part table.Hash}\u0000{part range}"
        | None -> part table.Hash

    let keyOf (table: FakeTable) (item: IDictionary<string, AttributeValue>) =
        let names = table.Hash :: (table.Range |> Option.toList)
        project names item

    let snapshotOf () =
        Dictionary(
            store
            |> Seq.map (fun table ->
                KeyValuePair(
                    table.Key,
                    Dictionary(table.Value |> Seq.map (fun item -> KeyValuePair(item.Key, cloneItem item.Value)))
                ))
        )

    let readView (consistent: bool) =
        match laggedView with
        | Some view when not consistent -> view
        | _ -> store

    let count name =
        calls[name] <- (match calls.TryGetValue name with | true, n -> n | _ -> 0) + 1

    let record (request: obj) =
        match request with
        | :? QueryRequest as query -> queries <- queries @ [ query ]
        | _ -> ()

    let intercept (request: obj) (apply: unit -> 'T) : 'T =
        match interceptor request with
        | Proceed -> apply ()
        | FailBefore error -> raise error
        | FailAfterCommit error ->
            apply () |> ignore
            raise error

    let asTask (work: unit -> 'T) : Task<'T> =
        try
            Task.FromResult(lock gate work)
        with error ->
            Task.FromException<'T>(error)

    /// DynamoDB rejects expression attribute names or values that no expression references.
    let validateReferences
        (names: IDictionary<string, string>)
        (values: IDictionary<string, AttributeValue>)
        (expressions: string list)
        =
        let text = expressions |> List.filter (isNull >> not) |> String.concat " "

        let referenced (prefix: string) =
            Text.RegularExpressions.Regex.Matches(text, prefix + "[A-Za-z0-9_]+")
            |> Seq.map _.Value
            |> Set.ofSeq

        let unused (provided: string seq) (prefix: string) =
            provided |> Seq.filter (fun key -> not ((referenced prefix).Contains key)) |> List.ofSeq

        match names |> Option.ofObj |> Option.map (fun n -> unused n.Keys "#") with
        | Some(first :: _) ->
            raise (AmazonDynamoDBException $"Value provided in ExpressionAttributeNames unused in expressions: keys: {{{first}}}")
        | _ -> ()

        match values |> Option.ofObj |> Option.map (fun v -> unused v.Keys ":") with
        | Some(first :: _) ->
            raise (AmazonDynamoDBException $"Value provided in ExpressionAttributeValues unused in expressions: keys: {{{first}}}")
        | _ -> ()

    let conditionHolds names values (expression: string) (item: IDictionary<string, AttributeValue>) =
        String.IsNullOrWhiteSpace expression
        || evaluateCondition values item (parseCondition names expression)

    let tryFind (table: FakeTable) (key: IDictionary<string, AttributeValue>) =
        match store[table.Name].TryGetValue(keyString table key) with
        | true, item -> Some item
        | _ -> None

    let sortValue (item: IDictionary<string, AttributeValue>) (name: string option) =
        name
        |> Option.bind (fun attribute ->
            match item.TryGetValue attribute with
            | true, value -> Some value
            | _ -> None)

    let compareAttribute (left: AttributeValue option) (right: AttributeValue option) =
        match left, right with
        | Some l, Some r when not (isNull l.N) && not (isNull r.N) -> compare (decimal l.N) (decimal r.N)
        | Some l, Some r -> String.CompareOrdinal(l.S, r.S) |> sign
        | None, None -> 0
        | None, Some _ -> -1
        | Some _, None -> 1

    let orderKey (table: FakeTable) (index: FakeIndex option) (item: IDictionary<string, AttributeValue>) =
        let range = index |> Option.map _.Range |> Option.defaultValue table.Range

        [ sortValue item range
          sortValue item (Some table.Hash)
          sortValue item table.Range ]

    let compareOrder (left: AttributeValue option list) (right: AttributeValue option list) =
        List.zip left right
        |> List.map (fun (l, r) -> compareAttribute l r)
        |> List.tryFind (fun result -> result <> 0)
        |> Option.defaultValue 0

    let projectForIndex (table: FakeTable) (index: FakeIndex option) (item: Dictionary<string, AttributeValue>) =
        match index with
        | Some { Projection = Some attributes } as definition ->
            let keys =
                [ table.Hash ]
                @ (table.Range |> Option.toList)
                @ [ definition.Value.Hash ]
                @ (definition.Value.Range |> Option.toList)

            project (keys @ attributes) item
        | _ -> cloneItem item

    /// The attributes of a query's `LastEvaluatedKey` and `ExclusiveStartKey`.
    let startKeyNames (table: FakeTable) (index: FakeIndex option) =
        [ table.Hash ]
        @ (table.Range |> Option.toList)
        @ (index |> Option.map (fun i -> i.Hash :: (i.Range |> Option.toList)) |> Option.defaultValue [])
        |> List.distinct

    /// DynamoDB rejects a start key that lacks or adds key attributes, or holds a value that is
    /// not a string or number. Value types are not checked against a schema.
    let validateStartKey (table: FakeTable) (index: FakeIndex option) (start: Dictionary<string, AttributeValue>) =
        let expected = startKeyNames table index |> Set.ofList
        let scalar (value: AttributeValue) = not (isNull value.S) || not (isNull value.N)

        if Set.ofSeq start.Keys <> expected || not (start.Values |> Seq.forall scalar) then
            raise (AmazonDynamoDBException "The provided starting key is invalid")

    let query (request: QueryRequest) =
        let table = tableOf request.TableName

        let index =
            request.IndexName
            |> Option.ofObj
            |> Option.map (fun name ->
                table.Indexes
                |> List.tryFind (fun index -> index.Name = name)
                |> Option.defaultWith (fun () ->
                    raise (AmazonDynamoDBException $"The table does not have the specified index: {name}")))

        let consistent = request.ConsistentRead.GetValueOrDefault false

        if index.IsSome && consistent then
            raise (AmazonDynamoDBException "Consistent reads are not supported on global secondary indexes")

        let source = readView consistent
        let names = request.ExpressionAttributeNames
        let values = request.ExpressionAttributeValues

        validateReferences
            names
            values
            [ request.KeyConditionExpression; request.FilterExpression; request.ProjectionExpression ]

        let keyCondition = parseCondition names request.KeyConditionExpression

        let inIndex (item: Dictionary<string, AttributeValue>) =
            match index with
            | Some definition ->
                item.ContainsKey definition.Hash
                && (definition.Range |> Option.forall item.ContainsKey)
            | None -> true

        let ordered =
            source[table.Name].Values
            |> Seq.filter inIndex
            |> Seq.filter (fun item -> evaluateCondition values item keyCondition)
            |> Seq.map (projectForIndex table index)
            |> Seq.sortWith (fun l r -> compareOrder (orderKey table index l) (orderKey table index r))
            |> Seq.toList

        let ordered =
            if request.ScanIndexForward.GetValueOrDefault true then
                ordered
            else
                List.rev ordered

        request.ExclusiveStartKey
        |> Option.ofObj
        |> Option.filter (fun start -> start.Count > 0)
        |> Option.iter (validateStartKey table index)

        let afterStart =
            match request.ExclusiveStartKey |> Option.ofObj with
            | Some start when start.Count > 0 ->
                let startOrder = orderKey table index start
                let forward = request.ScanIndexForward.GetValueOrDefault true

                ordered
                |> List.filter (fun item ->
                    let result = compareOrder (orderKey table index item) startOrder
                    if forward then result > 0 else result < 0)
            | _ -> ordered

        let limit =
            min (request.Limit.GetValueOrDefault Int32.MaxValue) (queryPageSize |> Option.defaultValue Int32.MaxValue)

        let evaluated = afterStart |> List.truncate limit
        let hasMore = afterStart.Length > evaluated.Length

        let filtered =
            match request.FilterExpression |> Option.ofObj with
            | Some filter when filter <> "" ->
                let condition = parseCondition names filter
                evaluated |> List.filter (fun item -> evaluateCondition values item condition)
            | _ -> evaluated

        let projected =
            match request.ProjectionExpression |> Option.ofObj with
            | Some projection when projection <> "" ->
                let attributes = parseProjection names projection
                filtered |> List.map (project attributes)
            | _ -> filtered

        let lastEvaluatedKey =
            match hasMore, emptyFinalKey with
            | true, _ -> project (startKeyNames table index) (evaluated |> List.last)
            | false, true -> Dictionary()
            | false, false -> null

        QueryResponse(
            Items = ResizeArray(projected |> List.map (fun item -> item :> Dictionary<string, AttributeValue>)),
            Count = projected.Length,
            ScannedCount = evaluated.Length,
            LastEvaluatedKey = lastEvaluatedKey
        )

    let conditionalFailure (item: Dictionary<string, AttributeValue> option) =
        let error = ConditionalCheckFailedException "The conditional request failed"
        item |> Option.iter (fun found -> error.Item <- cloneItem found)
        error

    let put (request: PutItemRequest) =
        validateReferences request.ExpressionAttributeNames request.ExpressionAttributeValues [ request.ConditionExpression ]
        let table = tableOf request.TableName
        let existing = tryFind table request.Item

        let holds =
            conditionHolds
                request.ExpressionAttributeNames
                request.ExpressionAttributeValues
                request.ConditionExpression
                (existing |> Option.defaultValue (Dictionary()))

        if not holds then
            raise (conditionalFailure existing)

        commit table (keyString table request.Item) (Some(cloneItem request.Item))
        PutItemResponse()

    let applyUpdateTo (table: FakeTable) key names values (updateExpression: string) =
        let existing = tryFind table key

        let working =
            existing
            |> Option.map cloneItem
            |> Option.defaultWith (fun () -> cloneItem key)

        applyUpdate values working (parseUpdate names updateExpression)
        commit table (keyString table key) (Some working)
        existing, working

    let update (request: UpdateItemRequest) =
        validateReferences
            request.ExpressionAttributeNames
            request.ExpressionAttributeValues
            [ request.ConditionExpression; request.UpdateExpression ]

        let table = tableOf request.TableName
        let existing = tryFind table request.Key

        let holds =
            conditionHolds
                request.ExpressionAttributeNames
                request.ExpressionAttributeValues
                request.ConditionExpression
                (existing |> Option.defaultValue (Dictionary()))

        if not holds then
            raise (conditionalFailure existing)

        let previous, current =
            applyUpdateTo
                table
                request.Key
                request.ExpressionAttributeNames
                request.ExpressionAttributeValues
                request.UpdateExpression

        let attributes =
            match request.ReturnValues |> Option.ofObj |> Option.map _.Value with
            | Some "ALL_NEW"
            | Some "UPDATED_NEW" -> cloneItem current
            | Some "ALL_OLD"
            | Some "UPDATED_OLD" -> previous |> Option.map cloneItem |> Option.defaultValue (Dictionary())
            | _ -> null

        UpdateItemResponse(Attributes = attributes)

    let delete (request: DeleteItemRequest) =
        validateReferences request.ExpressionAttributeNames request.ExpressionAttributeValues [ request.ConditionExpression ]
        let table = tableOf request.TableName
        let existing = tryFind table request.Key

        let holds =
            conditionHolds
                request.ExpressionAttributeNames
                request.ExpressionAttributeValues
                request.ConditionExpression
                (existing |> Option.defaultValue (Dictionary()))

        if not holds then
            raise (conditionalFailure existing)

        commit table (keyString table request.Key) None
        DeleteItemResponse()

    let getItem (request: GetItemRequest) =
        validateReferences request.ExpressionAttributeNames null [ request.ProjectionExpression ]
        let table = tableOf request.TableName
        let source = readView (request.ConsistentRead.GetValueOrDefault false)

        match source[table.Name].TryGetValue(keyString table request.Key) with
        | true, item ->
            let projected =
                match request.ProjectionExpression |> Option.ofObj with
                | Some projection when projection <> "" ->
                    project (parseProjection request.ExpressionAttributeNames projection) item
                | _ -> cloneItem item

            GetItemResponse(Item = projected, IsItemSet = true)
        | _ -> GetItemResponse(Item = null, IsItemSet = false)

    let batchGet (request: BatchGetItemRequest) =
        let responses = Dictionary<string, List<Dictionary<string, AttributeValue>>>()
        let unprocessed = Dictionary<string, KeysAndAttributes>()

        for pair in request.RequestItems do
            let table = tableOf pair.Key
            let source = readView (pair.Value.ConsistentRead.GetValueOrDefault false)
            let keys = pair.Value.Keys |> List.ofSeq
            let withheld = min unprocessedBatchGetKeys keys.Length
            unprocessedBatchGetKeys <- unprocessedBatchGetKeys - withheld
            let served, deferred = List.splitAt (keys.Length - withheld) keys

            let found =
                served
                |> List.choose (fun key ->
                    match source[table.Name].TryGetValue(keyString table key) with
                    | true, item ->
                        match pair.Value.ProjectionExpression |> Option.ofObj with
                        | Some projection when projection <> "" ->
                            Some(project (parseProjection pair.Value.ExpressionAttributeNames projection) item)
                        | _ -> Some(cloneItem item)
                    | _ -> None)

            responses[pair.Key] <- List(found)

            if not deferred.IsEmpty then
                unprocessed[pair.Key] <-
                    KeysAndAttributes(
                        Keys = List(deferred),
                        ConsistentRead = pair.Value.ConsistentRead,
                        ProjectionExpression = pair.Value.ProjectionExpression,
                        ExpressionAttributeNames = pair.Value.ExpressionAttributeNames
                    )

        BatchGetItemResponse(Responses = responses, UnprocessedKeys = unprocessed)

    let batchWrite (request: BatchWriteItemRequest) =
        for pair in request.RequestItems do
            let table = tableOf pair.Key

            for write in pair.Value do
                if not (isNull write.PutRequest) then
                    commit table (keyString table write.PutRequest.Item) (Some(cloneItem write.PutRequest.Item))
                elif not (isNull write.DeleteRequest) then
                    commit table (keyString table write.DeleteRequest.Key) None

        BatchWriteItemResponse(UnprocessedItems = Dictionary())

    let transact (request: TransactWriteItemsRequest) =
        let items = request.TransactItems |> List.ofSeq

        if items.Length > 100 then
            raise (AmazonDynamoDBException "Member must have length less than or equal to 100")

        let describe (item: TransactWriteItem) =
            if not (isNull item.Put) then
                let table = tableOf item.Put.TableName
                table, keyOf table item.Put.Item, item.Put.ConditionExpression, item.Put.ExpressionAttributeNames, item.Put.ExpressionAttributeValues, item.Put.ReturnValuesOnConditionCheckFailure
            elif not (isNull item.Update) then
                let table = tableOf item.Update.TableName
                table, item.Update.Key, item.Update.ConditionExpression, item.Update.ExpressionAttributeNames, item.Update.ExpressionAttributeValues, item.Update.ReturnValuesOnConditionCheckFailure
            elif not (isNull item.Delete) then
                let table = tableOf item.Delete.TableName
                table, item.Delete.Key, item.Delete.ConditionExpression, item.Delete.ExpressionAttributeNames, item.Delete.ExpressionAttributeValues, item.Delete.ReturnValuesOnConditionCheckFailure
            else
                let table = tableOf item.ConditionCheck.TableName
                table, item.ConditionCheck.Key, item.ConditionCheck.ConditionExpression, item.ConditionCheck.ExpressionAttributeNames, item.ConditionCheck.ExpressionAttributeValues, item.ConditionCheck.ReturnValuesOnConditionCheckFailure

        for item in items do
            if not (isNull item.Update) then
                validateReferences
                    item.Update.ExpressionAttributeNames
                    item.Update.ExpressionAttributeValues
                    [ item.Update.ConditionExpression; item.Update.UpdateExpression ]
            elif not (isNull item.Put) then
                validateReferences item.Put.ExpressionAttributeNames item.Put.ExpressionAttributeValues [ item.Put.ConditionExpression ]
            elif not (isNull item.Delete) then
                validateReferences item.Delete.ExpressionAttributeNames item.Delete.ExpressionAttributeValues [ item.Delete.ConditionExpression ]
            else
                validateReferences
                    item.ConditionCheck.ExpressionAttributeNames
                    item.ConditionCheck.ExpressionAttributeValues
                    [ item.ConditionCheck.ConditionExpression ]

        let described = items |> List.map describe

        let distinctKeys =
            described
            |> List.map (fun (table, key, _, _, _, _) -> table.Name, keyString table key)
            |> List.distinct

        if distinctKeys.Length <> described.Length then
            raise (AmazonDynamoDBException "Transaction request cannot include multiple operations on one item")

        let reasons =
            described
            |> List.map (fun (table, key, condition, names, values, returnValues) ->
                let existing = tryFind table key

                if conditionHolds names values condition (existing |> Option.defaultValue (Dictionary())) then
                    CancellationReason(Code = "None")
                else
                    let reason = CancellationReason(Code = "ConditionalCheckFailed", Message = "The conditional request failed")

                    match returnValues |> Option.ofObj |> Option.map _.Value, existing with
                    | Some "ALL_OLD", Some found -> reason.Item <- cloneItem found
                    | _ -> ()

                    reason)

        if reasons |> List.exists (fun reason -> reason.Code <> "None") then
            let codes = reasons |> List.map _.Code |> String.concat ", "

            raise (
                TransactionCanceledException(
                    $"Transaction cancelled, please refer cancellation reasons for specific reasons [{codes}]",
                    CancellationReasons = List(reasons)
                )
            )

        for item in items do
            if not (isNull item.Put) then
                let table = tableOf item.Put.TableName
                commit table (keyString table item.Put.Item) (Some(cloneItem item.Put.Item))
            elif not (isNull item.Update) then
                let table = tableOf item.Update.TableName

                applyUpdateTo
                    table
                    item.Update.Key
                    item.Update.ExpressionAttributeNames
                    item.Update.ExpressionAttributeValues
                    item.Update.UpdateExpression
                |> ignore
            elif not (isNull item.Delete) then
                let table = tableOf item.Delete.TableName
                commit table (keyString table item.Delete.Key) None

        TransactWriteItemsResponse()

    let mock =
        let client = Mock<IAmazonDynamoDB>(MockBehavior.Strict)

        let handle name (request: obj) work =
            asTask (fun () ->
                count name
                record request
                intercept request work)

        client
            .Setup(fun c -> c.QueryAsync(It.IsAny<QueryRequest>(), It.IsAny<CancellationToken>()))
            .Returns(fun (request: QueryRequest) (_: CancellationToken) -> handle "Query" request (fun () -> query request))
        |> ignore

        client
            .Setup(fun c -> c.GetItemAsync(It.IsAny<GetItemRequest>(), It.IsAny<CancellationToken>()))
            .Returns(fun (request: GetItemRequest) (_: CancellationToken) -> handle "GetItem" request (fun () -> getItem request))
        |> ignore

        client
            .Setup(fun c -> c.PutItemAsync(It.IsAny<PutItemRequest>(), It.IsAny<CancellationToken>()))
            .Returns(fun (request: PutItemRequest) (_: CancellationToken) -> handle "PutItem" request (fun () -> put request))
        |> ignore

        client
            .Setup(fun c -> c.UpdateItemAsync(It.IsAny<UpdateItemRequest>(), It.IsAny<CancellationToken>()))
            .Returns(fun (request: UpdateItemRequest) (_: CancellationToken) ->
                handle "UpdateItem" request (fun () -> update request))
        |> ignore

        client
            .Setup(fun c -> c.DeleteItemAsync(It.IsAny<DeleteItemRequest>(), It.IsAny<CancellationToken>()))
            .Returns(fun (request: DeleteItemRequest) (_: CancellationToken) ->
                handle "DeleteItem" request (fun () -> delete request))
        |> ignore

        client
            .Setup(fun c -> c.BatchGetItemAsync(It.IsAny<BatchGetItemRequest>(), It.IsAny<CancellationToken>()))
            .Returns(fun (request: BatchGetItemRequest) (_: CancellationToken) ->
                handle "BatchGetItem" request (fun () -> batchGet request))
        |> ignore

        client
            .Setup(fun c -> c.BatchWriteItemAsync(It.IsAny<BatchWriteItemRequest>(), It.IsAny<CancellationToken>()))
            .Returns(fun (request: BatchWriteItemRequest) (_: CancellationToken) ->
                handle "BatchWriteItem" request (fun () -> batchWrite request))
        |> ignore

        client
            .Setup(fun c ->
                c.TransactWriteItemsAsync(It.IsAny<TransactWriteItemsRequest>(), It.IsAny<CancellationToken>()))
            .Returns(fun (request: TransactWriteItemsRequest) (_: CancellationToken) ->
                handle "TransactWriteItems" request (fun () -> transact request))
        |> ignore

        client

    member _.Client: IAmazonDynamoDB = mock.Object

    /// Seeds or replaces an item directly, bypassing conditions and faults.
    member _.Put(tableName: string, item: IDictionary<string, AttributeValue>) =
        lock gate (fun () ->
            let table = tableOf tableName
            store[table.Name][keyString table item] <- cloneItem item)

    member _.TryGet(tableName: string, key: IDictionary<string, AttributeValue>) =
        lock gate (fun () ->
            let table = tableOf tableName
            tryFind table key |> Option.map cloneItem)

    member _.Items(tableName: string) =
        lock gate (fun () -> store[(tableOf tableName).Name].Values |> Seq.map cloneItem |> List.ofSeq)

    /// Freezes index and eventually consistent reads at the current state until SyncIndexes.
    member _.FreezeIndexes() = lock gate (fun () -> laggedView <- Some(snapshotOf ()))

    member _.SyncIndexes() =
        lock gate (fun () ->
            if laggedView.IsSome then
                laggedView <- Some(snapshotOf ()))

    member _.ReleaseIndexes() = lock gate (fun () -> laggedView <- None)

    /// Decides the fate of each request. The request object is the SDK request instance.
    member _.Intercept(handler: obj -> FakeFault) = lock gate (fun () -> interceptor <- handler)

    member _.ClearInterception() = lock gate (fun () -> interceptor <- fun _ -> Proceed)

    /// Withholds the given number of keys from the next BatchGetItem responses as unprocessed keys.
    member _.WithholdBatchGetKeys(count: int) = lock gate (fun () -> unprocessedBatchGetKeys <- count)

    /// Returns and forgets the changes committed through the client since the last call.
    member _.TakeChanges() =
        lock gate (fun () ->
            let taken = changes
            changes <- []
            taken)

    member _.HasPendingChanges = lock gate (fun () -> not changes.IsEmpty)

    /// Caps every query's storage page at `pageSize` evaluated items, as a small `Limit`
    /// would, without changing the request the code under test built.
    member _.LimitQueryPages(pageSize: int) = lock gate (fun () -> queryPageSize <- Some pageSize)

    /// Ends complete queries with an empty `LastEvaluatedKey` map instead of none.
    member _.EndQueriesWithEmptyKey() = lock gate (fun () -> emptyFinalKey <- true)

    /// Returns and forgets the query requests received since the last call, including ones
    /// an interception failed.
    member _.TakeQueries() =
        lock gate (fun () ->
            let taken = queries
            queries <- []
            taken)

    member _.CallCount(name: string) =
        lock gate (fun () -> match calls.TryGetValue name with | true, n -> n | _ -> 0)
