module PoolLayoutPaginationTest

#nowarn "3536"

open System.Collections.Generic
open System.Threading
open System.Threading.Tasks
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Tyche.PoolLayout.Infrastructure
open Felipearpa.Type
open FsUnit.Xunit
open Moq
open Xunit

let private s (value: string) = AttributeValue(S = value)
let private layoutId = "01K1PX1TX2NM1HG851S1V0Y001"
let private otherLayoutId = "01K1PX1TX2NM1HG851S1V0Y002"

let private layoutRoot =
    Dictionary(
        dict
            [ "pk", s $"POOLLAYOUT#{layoutId}"
              "sk", s $"POOLLAYOUT#{layoutId}"
              "poolLayoutId", s layoutId
              "poolLayoutName", s "Mundial"
              "status", s "OPENED"
              "startDateTime", s "2026-06-11T00:00:00Z" ]
    )

let private openLayoutKey =
    Dictionary(
        dict
            [ "pk", s $"POOLLAYOUT#{otherLayoutId}"
              "sk", s $"POOLLAYOUT#{otherLayoutId}"
              "status", s "OPENED"
              "startDateTime", s "2026-06-10T00:00:00Z" ]
    )

let private describe (key: IDictionary<string, AttributeValue>) =
    key |> Seq.map (fun entry -> entry.Key, entry.Value.S) |> Map.ofSeq

/// A client answering queries with `responses` in order.
let private scripted (responses: QueryResponse list) =
    let client = Mock<IAmazonDynamoDB>(MockBehavior.Strict)

    responses
    |> List.fold
        (fun (setup: Language.ISetupSequentialResult<Task<QueryResponse>>) response -> setup.Returns(Task.FromResult response))
        (client.SetupSequence(fun c -> c.QueryAsync(It.IsAny<QueryRequest>(), It.IsAny<CancellationToken>())))
    |> ignore

    client

let private sentRequests (client: Mock<IAmazonDynamoDB>) =
    client.Invocations |> Seq.map (fun invocation -> invocation.Arguments[0] :?> QueryRequest) |> List.ofSeq

let private repository (serializer: IKeySerializer) (client: Mock<IAmazonDynamoDB>) =
    PoolLayoutDynamoDbRepository(serializer, client.Object) :> IPoolLayoutRepository

let private invalidContinuationOf (work: Async<'T>) =
    match work |> Async.Catch |> Async.RunSynchronously with
    | Choice2Of2(InvalidContinuationException reason) -> reason
    | other -> failwith $"expected an invalid continuation, got %A{other}"

[<Fact>]
let ``given an open-layouts page with no items and a key when continued then the next request starts at that key`` () =
    let client =
        scripted
            [ QueryResponse(Items = List(), LastEvaluatedKey = openLayoutKey)
              QueryResponse(Items = List([ layoutRoot ]), LastEvaluatedKey = Dictionary()) ]

    let layouts = repository (DynamoDbKeySerializer()) client

    let first = layouts.GetOpenPoolLayoutsAsync None |> Async.RunSynchronously
    let second = layouts.GetOpenPoolLayoutsAsync first.Next |> Async.RunSynchronously

    first.Items |> Seq.isEmpty |> should equal true
    first.Next.IsSome |> should equal true
    second.Items |> Seq.map _.Id.Value |> List.ofSeq |> should equal [ layoutId ]
    second.Next |> should equal None

    sentRequests client
    |> List.map (fun request -> request.ExclusiveStartKey |> Option.ofObj |> Option.map describe)
    |> should equal [ None; Some(describe openLayoutKey) ]

[<Fact>]
let ``given an absent final key when open layouts are read then there is no next token`` () =
    let client = scripted [ QueryResponse(Items = List([ layoutRoot ]), LastEvaluatedKey = null) ]

    let page = repository (DynamoDbKeySerializer()) client |> _.GetOpenPoolLayoutsAsync(None) |> Async.RunSynchronously

    page.Next |> should equal None

[<Fact>]
let ``given a legacy pk and sk token when open layouts are continued then it is rejected before querying`` () =
    let client = scripted []
    let legacy = $"""{{"pk":"POOLLAYOUT#{otherLayoutId}","sk":"POOLLAYOUT#{otherLayoutId}"}}"""

    let reason =
        repository (DynamoDbKeySerializer()) client
        |> _.GetOpenPoolLayoutsAsync(Some legacy)
        |> invalidContinuationOf

    reason |> should equal (MissingKeyAttribute "startDateTime")
    sentRequests client |> should be Empty

[<Fact>]
let ``given a legacy pk and sk token when layout matches are continued then the query starts at that key`` () =
    let client = scripted [ QueryResponse(Items = List(), LastEvaluatedKey = Dictionary()) ]
    let matchKey = $"MATCH#01K1PX1TX2NM1HG851S1V0M004"
    let legacy = $"""{{"pk":"POOLLAYOUT#{layoutId}","sk":"{matchKey}"}}"""

    let page =
        repository (DynamoDbKeySerializer()) client
        |> _.GetPoolLayoutMatchesThroughVersionAsync(Ulid.newOf layoutId, 1, Some legacy)
        |> Async.RunSynchronously

    page.Next |> should equal None

    sentRequests client
    |> List.map (fun request -> describe request.ExclusiveStartKey)
    |> should equal [ Map [ "pk", $"POOLLAYOUT#{layoutId}"; "sk", matchKey ] ]

[<Fact>]
let ``given a filtered layout-match page with a key when continued then the next request starts at that key`` () =
    let boundary = Dictionary(dict [ "pk", s $"POOLLAYOUT#{layoutId}"; "sk", s "MATCH#01K1PX1TX2NM1HG851S1V0M002" ])

    let client =
        scripted
            [ QueryResponse(Items = List(), LastEvaluatedKey = boundary)
              QueryResponse(Items = List(), LastEvaluatedKey = null) ]

    let layouts = repository (DynamoDbKeySerializer()) client

    let first =
        layouts.GetPoolLayoutMatchesThroughVersionAsync(Ulid.newOf layoutId, 1, None) |> Async.RunSynchronously

    let second =
        layouts.GetPoolLayoutMatchesThroughVersionAsync(Ulid.newOf layoutId, 1, first.Next)
        |> Async.RunSynchronously

    first.Next.IsSome |> should equal true
    second.Next |> should equal None

    sentRequests client
    |> List.map (fun request -> request.ExclusiveStartKey |> Option.ofObj |> Option.map describe)
    |> should equal [ None; Some(describe boundary) ]

[<Fact>]
let ``given another layout's token when layout matches are continued then it is rejected before querying`` () =
    let client = scripted []
    let legacy = $"""{{"pk":"POOLLAYOUT#{otherLayoutId}","sk":"MATCH#01K1PX1TX2NM1HG851S1V0M004"}}"""

    let reason =
        repository (DynamoDbKeySerializer()) client
        |> _.GetPoolLayoutMatchesThroughVersionAsync(Ulid.newOf layoutId, 1, Some legacy)
        |> invalidContinuationOf

    reason |> should equal (KeyOutOfScope("pk", "the value belongs to another partition or key"))
    sentRequests client |> should be Empty
