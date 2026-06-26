namespace Felipearpa.Tyche.AmazonLambda.Event.Tests

open System.Collections.Generic
open Amazon.Lambda.DynamoDBEvents
open Felipearpa.Tyche.AmazonLambda.Event
open Felipearpa.Type
open FsUnit.Xunit
open Xunit

module MatchScoreIngestionEventFilterTest =

    let private matchId = "01HY8V7VXGPNN8CS5QY8AVMZ2C"
    let private poolLayoutId = "01HY8V7VXGPNN8CS5QY8AVMZ2D"
    let private externalMatchId = "537327"
    let private matchDateTime = "2026-06-12T17:00:00Z"

    let private makeRecord
        (eventName: string)
        (oldImage: IDictionary<string, DynamoDBEvent.AttributeValue>)
        (newImage: IDictionary<string, DynamoDBEvent.AttributeValue>)
        =
        let streamRecord = DynamoDBEvent.StreamRecord()
        streamRecord.OldImage <- Dictionary(oldImage)
        streamRecord.NewImage <- Dictionary(newImage)

        let record = DynamoDBEvent.DynamodbStreamRecord()
        record.EventName <- eventName
        record.Dynamodb <- streamRecord
        record

    let private makeEvent (records: DynamoDBEvent.DynamodbStreamRecord list) =
        let event = DynamoDBEvent()
        event.Records <- ResizeArray(records)
        event

    let private stringAttr value = DynamoDBEvent.AttributeValue(S = value)

    let private fullRow extra =
        dict (
            [ "matchId", stringAttr matchId
              "poolLayoutId", stringAttr poolLayoutId
              "externalMatchId", stringAttr externalMatchId
              "matchDateTime", stringAttr matchDateTime ]
            @ extra
        )

    let private ``given a control row insert`` () =
        makeRecord "INSERT" (dict []) (fullRow [ "status", stringAttr "PENDING" ])

    [<Fact>]
    let ``given an insert when extracting schedules to create then one schedule is returned`` () =
        let event = ``given a control row insert`` () |> List.singleton |> makeEvent

        let result = MatchScoreIngestionEventFilter.extractSchedulesToCreate event |> Seq.toList

        result |> should haveLength 1
        result[0].MatchId |> should equal (Ulid.newOf matchId)
        result[0].PoolLayoutId |> should equal (Ulid.newOf poolLayoutId)
        result[0].ExternalMatchId |> should equal externalMatchId

    [<Fact>]
    let ``given an insert missing externalMatchId when extracting schedules to create then nothing is returned`` () =
        let record =
            makeRecord
                "INSERT"
                (dict [])
                (dict
                    [ "matchId", stringAttr matchId
                      "poolLayoutId", stringAttr poolLayoutId
                      "matchDateTime", stringAttr matchDateTime ])

        let event = record |> List.singleton |> makeEvent

        let result = MatchScoreIngestionEventFilter.extractSchedulesToCreate event |> Seq.toList

        result |> should be Empty

    [<Fact>]
    let ``given a remove when extracting schedules to create then nothing is returned`` () =
        let event =
            makeRecord "REMOVE" (fullRow []) (dict []) |> List.singleton |> makeEvent

        let result = MatchScoreIngestionEventFilter.extractSchedulesToCreate event |> Seq.toList

        result |> should be Empty

    [<Fact>]
    let ``given a remove when extracting schedules to delete then the matchId is returned`` () =
        let event =
            makeRecord "REMOVE" (fullRow [ "status", stringAttr "PENDING" ]) (dict [])
            |> List.singleton
            |> makeEvent

        let result = MatchScoreIngestionEventFilter.extractSchedulesToDelete event |> Seq.toList

        result |> should haveLength 1
        result[0] |> should equal (Ulid.newOf matchId)

    [<Fact>]
    let ``given a transition to completed when extracting schedules to delete then the matchId is returned`` () =
        let event =
            makeRecord "MODIFY" (fullRow [ "status", stringAttr "PENDING" ]) (fullRow [ "status", stringAttr "COMPLETED" ])
            |> List.singleton
            |> makeEvent

        let result = MatchScoreIngestionEventFilter.extractSchedulesToDelete event |> Seq.toList

        result |> should haveLength 1
        result[0] |> should equal (Ulid.newOf matchId)

    [<Fact>]
    let ``given a transition to expired when extracting schedules to delete then the matchId is returned`` () =
        let event =
            makeRecord "MODIFY" (fullRow [ "status", stringAttr "PENDING" ]) (fullRow [ "status", stringAttr "EXPIRED" ])
            |> List.singleton
            |> makeEvent

        let result = MatchScoreIngestionEventFilter.extractSchedulesToDelete event |> Seq.toList

        result |> should haveLength 1
        result[0] |> should equal (Ulid.newOf matchId)

    [<Fact>]
    let ``given a poll tick modify that keeps status pending when extracting schedules to delete then nothing is returned`` () =
        let event =
            makeRecord
                "MODIFY"
                (fullRow [ "status", stringAttr "PENDING"; "pollCount", DynamoDBEvent.AttributeValue(N = "1") ])
                (fullRow [ "status", stringAttr "PENDING"; "pollCount", DynamoDBEvent.AttributeValue(N = "2") ])
            |> List.singleton
            |> makeEvent

        let result = MatchScoreIngestionEventFilter.extractSchedulesToDelete event |> Seq.toList

        result |> should be Empty

    [<Fact>]
    let ``given a modify already completed when extracting schedules to delete then nothing is returned`` () =
        let event =
            makeRecord "MODIFY" (fullRow [ "status", stringAttr "COMPLETED" ]) (fullRow [ "status", stringAttr "COMPLETED" ])
            |> List.singleton
            |> makeEvent

        let result = MatchScoreIngestionEventFilter.extractSchedulesToDelete event |> Seq.toList

        result |> should be Empty
