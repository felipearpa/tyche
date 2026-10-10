namespace Felipearpa.Tyche.Pool.Test.Fakes

open System
open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open FsUnitTyped
open Xunit

/// Checks the fake's conditional, transactional, and index semantics that the settlement
/// tests rely on.
module FakeDynamoDbTest =

    let private s (value: string) = AttributeValue(S = value)
    let private n (value: int) = AttributeValue(N = string value)

    let private run (task: System.Threading.Tasks.Task<'T>) =
        try
            Ok(task.GetAwaiter().GetResult())
        with error ->
            Error error

    let private item pk sk (extra: (string * AttributeValue) list) =
        Dictionary(dict ([ "pk", s pk; "sk", s sk ] @ extra))

    [<Fact>]
    let ``a failed transaction condition applies none of its writes`` () =
        let fake = FakeTables.create ()
        fake.Put("Pool", item "P" "A" [ "score", n 1 ])
        fake.Put("Pool", item "P" "B" [ "score", n 1 ])

        let request =
            TransactWriteItemsRequest(
                TransactItems =
                    List(
                        [ TransactWriteItem(
                              Update =
                                  Update(
                                      TableName = "Pool",
                                      Key = Dictionary(dict [ "pk", s "P"; "sk", s "A" ]),
                                      UpdateExpression = "ADD #score :one",
                                      ExpressionAttributeNames = Dictionary(dict [ "#score", "score" ]),
                                      ExpressionAttributeValues = Dictionary(dict [ ":one", n 1 ])
                                  )
                          )
                          TransactWriteItem(
                              ConditionCheck =
                                  ConditionCheck(
                                      TableName = "Pool",
                                      Key = Dictionary(dict [ "pk", s "P"; "sk", s "B" ]),
                                      ConditionExpression = "#score = :two",
                                      ExpressionAttributeNames = Dictionary(dict [ "#score", "score" ]),
                                      ExpressionAttributeValues = Dictionary(dict [ ":two", n 2 ])
                                  )
                          ) ]
                    )
            )

        let result = fake.Client.TransactWriteItemsAsync(request) |> run

        match result with
        | Error(:? TransactionCanceledException as error) ->
            error.CancellationReasons |> Seq.map _.Code |> List.ofSeq |> shouldEqual [ "None"; "ConditionalCheckFailed" ]
        | other -> failwith $"unexpected {other}"

        fake.TryGet("Pool", dict [ "pk", s "P"; "sk", s "A" ]).Value["score"].N |> shouldEqual "1"

    [<Fact>]
    let ``update expressions read the pre-update item and support nested paths`` () =
        let fake = FakeTables.create ()
        fake.Put("Pool", item "P" "A" [ "score", n 7; "active", AttributeValue(M = Dictionary(dict [ "owner", s "x" ])) ])

        let request =
            UpdateItemRequest(
                TableName = "Pool",
                Key = Dictionary(dict [ "pk", s "P"; "sk", s "A" ]),
                UpdateExpression = "SET #before = #score, #score = #score + :d, #active.#owner = :y REMOVE #gone",
                ConditionExpression = "#active.#owner = :x AND attribute_not_exists(#gone)",
                ExpressionAttributeNames =
                    Dictionary(
                        dict [ "#before", "beforeScore"; "#score", "score"; "#active", "active"; "#owner", "owner"; "#gone", "gone" ]
                    ),
                ExpressionAttributeValues = Dictionary(dict [ ":d", n 3; ":x", s "x"; ":y", s "y" ])
            )

        fake.Client.UpdateItemAsync(request) |> run |> Result.isOk |> shouldEqual true
        let stored = fake.TryGet("Pool", dict [ "pk", s "P"; "sk", s "A" ]) |> Option.get
        stored["beforeScore"].N |> shouldEqual "7"
        stored["score"].N |> shouldEqual "10"
        stored["active"].M["owner"].S |> shouldEqual "y"

    [<Fact>]
    let ``frozen indexes hide later writes from index and eventually consistent reads only`` () =
        let fake = FakeTables.create ()
        fake.FreezeIndexes()
        fake.Put("Pool", item "POOL#1" "GAMBLER#1" [ "score", n 3 ])

        let query consistent indexName =
            QueryRequest(
                TableName = "Pool",
                IndexName = indexName,
                ConsistentRead = consistent,
                KeyConditionExpression = "#pk = :pk",
                ExpressionAttributeNames = Dictionary(dict [ "#pk", "pk" ]),
                ExpressionAttributeValues = Dictionary(dict [ ":pk", s "POOL#1" ])
            )

        let countOf request =
            match fake.Client.QueryAsync(request) |> run with
            | Ok response -> response.Count.Value
            | Error error -> raise error

        countOf (query (Nullable true) null) |> shouldEqual 1
        countOf (query (Nullable false) null) |> shouldEqual 0
        countOf (query (Nullable()) "GetPoolGamblerScoresByPool-index") |> shouldEqual 0
        fake.SyncIndexes()
        countOf (query (Nullable()) "GetPoolGamblerScoresByPool-index") |> shouldEqual 1

    [<Fact>]
    let ``paged queries continue from the last evaluated key without gaps`` () =
        let fake = FakeTables.create ()

        for index in 1..5 do
            fake.Put("Pool", item "POOL#1" $"GAMBLER#{index}" [])

        let rec collect (start: Dictionary<string, AttributeValue>) acc =
            let request =
                QueryRequest(
                    TableName = "Pool",
                    ConsistentRead = true,
                    Limit = 2,
                    KeyConditionExpression = "#pk = :pk AND begins_with(#sk, :prefix)",
                    ExpressionAttributeNames = Dictionary(dict [ "#pk", "pk"; "#sk", "sk" ]),
                    ExpressionAttributeValues = Dictionary(dict [ ":pk", s "POOL#1"; ":prefix", s "GAMBLER#" ]),
                    ExclusiveStartKey = start
                )

            match fake.Client.QueryAsync(request) |> run with
            | Ok response ->
                let acc = acc @ (response.Items |> Seq.map (fun i -> i["sk"].S) |> List.ofSeq)

                if isNull response.LastEvaluatedKey then acc else collect response.LastEvaluatedKey acc
            | Error error -> raise error

        collect null [] |> shouldEqual [ for index in 1..5 -> $"GAMBLER#{index}" ]
