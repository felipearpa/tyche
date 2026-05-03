namespace Felipearpa.Tyche.AmazonLambda.Event.Tests

open System
open System.Collections.Generic
open Amazon.Lambda.DynamoDBEvents
open Felipearpa.Tyche.AmazonLambda.Event
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type
open FsUnit.Xunit
open Xunit

module PoolLayoutEventFilterTest =

    let private matchId = "01HY8V7VXGPNN8CS5QY8AVMZ2C"

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

    let private intAttr (value: int) =
        DynamoDBEvent.AttributeValue(N = string value)

    let private ``given an update record with new scores and no old scores`` () =
        makeRecord
            "MODIFY"
            (dict [])
            (dict
                [ "matchId", stringAttr matchId
                  "homeTeamScore", intAttr 2
                  "awayTeamScore", intAttr 1 ])

    let private ``given an update record with both old and new scores`` () =
        makeRecord
            "MODIFY"
            (dict [ "homeTeamScore", intAttr 0; "awayTeamScore", intAttr 0 ])
            (dict
                [ "matchId", stringAttr matchId
                  "homeTeamScore", intAttr 2
                  "awayTeamScore", intAttr 1 ])

    let private ``given an insert record`` () =
        makeRecord
            "INSERT"
            (dict [])
            (dict
                [ "matchId", stringAttr matchId
                  "homeTeamScore", intAttr 2
                  "awayTeamScore", intAttr 1 ])

    let private ``given an update record without matchId`` () =
        makeRecord "MODIFY" (dict []) (dict [ "homeTeamScore", intAttr 2; "awayTeamScore", intAttr 1 ])

    let private ``given an update record with new scores but missing away score`` () =
        makeRecord "MODIFY" (dict []) (dict [ "matchId", stringAttr matchId; "homeTeamScore", intAttr 2 ])

    [<Fact>]
    let ``given an update with new scores when extracting then the match is returned`` () =
        let event =
            ``given an update record with new scores and no old scores`` ()
            |> List.singleton
            |> makeEvent

        let result = PoolLayoutEventFilter.extractUpdatedPoolLayouts event |> Seq.toList

        result |> should haveLength 1

        let actualMatchId, actualScore = result[0]

        actualMatchId |> should equal (Ulid.newOf matchId)

        actualScore
        |> should
            equal
            { TeamScore.HomeTeamValue = 2
              AwayTeamValue = 1 }

    [<Fact>]
    let ``given an update with both old and new scores when extracting then nothing is returned`` () =
        let event =
            ``given an update record with both old and new scores`` ()
            |> List.singleton
            |> makeEvent

        let result = PoolLayoutEventFilter.extractUpdatedPoolLayouts event |> Seq.toList

        result |> should be Empty

    [<Fact>]
    let ``given an insert record when extracting then nothing is returned`` () =
        let event = ``given an insert record`` () |> List.singleton |> makeEvent

        let result = PoolLayoutEventFilter.extractUpdatedPoolLayouts event |> Seq.toList

        result |> should be Empty

    [<Fact>]
    let ``given an update without matchId when extracting then nothing is returned`` () =
        let event =
            ``given an update record without matchId`` () |> List.singleton |> makeEvent

        let result = PoolLayoutEventFilter.extractUpdatedPoolLayouts event |> Seq.toList

        result |> should be Empty

    [<Fact>]
    let ``given an update with missing away score when extracting then nothing is returned`` () =
        let event =
            ``given an update record with new scores but missing away score`` ()
            |> List.singleton
            |> makeEvent

        let result = PoolLayoutEventFilter.extractUpdatedPoolLayouts event |> Seq.toList

        result |> should be Empty

    [<Fact>]
    let ``given mixed records when extracting then only valid updates are returned`` () =
        let event =
            [ ``given an update record with new scores and no old scores`` ()
              ``given an insert record`` ()
              ``given an update record with both old and new scores`` ()
              ``given an update record without matchId`` () ]
            |> makeEvent

        let result = PoolLayoutEventFilter.extractUpdatedPoolLayouts event |> Seq.toList

        result |> should haveLength 1

    let private poolLayoutId = "01HY8V7VXGPNN8CS5QY8AVMZ2D"
    let private homeTeamId = "01HY8V7VXGPNN8CS5QY8AVMZ2E"
    let private awayTeamId = "01HY8V7VXGPNN8CS5QY8AVMZ2F"
    let private matchDateTime = "2026-05-01T00:00:00.000Z"

    let private ``given an insert match record`` () =
        makeRecord
            "INSERT"
            (dict [])
            (dict
                [ "matchId", stringAttr matchId
                  "poolLayoutId", stringAttr poolLayoutId
                  "poolLayoutVersion", intAttr 2
                  "homeTeamId", stringAttr homeTeamId
                  "homeTeamName", stringAttr "Tigre"
                  "awayTeamId", stringAttr awayTeamId
                  "awayTeamName", stringAttr "América"
                  "matchDateTime", stringAttr matchDateTime ])

    [<Fact>]
    let ``given an insert match record when extracting inserted matches then it is returned`` () =
        let event = ``given an insert match record`` () |> List.singleton |> makeEvent

        let result = PoolLayoutEventFilter.extractInsertedMatches event |> Seq.toList

        result |> should haveLength 1
        let input = result[0]
        input.MatchId |> should equal (Ulid.newOf matchId)
        input.PoolLayoutId |> should equal (Ulid.newOf poolLayoutId)
        input.PoolLayoutVersion |> should equal 2
        input.HomeTeamName.Value |> should equal "Tigre"
        input.AwayTeamName.Value |> should equal "América"
        input.MatchDateTime |> should equal (DateTime.Parse(matchDateTime))

    [<Fact>]
    let ``given a modify record when extracting inserted matches then nothing is returned`` () =
        let event =
            ``given an update record with new scores and no old scores`` ()
            |> List.singleton
            |> makeEvent

        let result = PoolLayoutEventFilter.extractInsertedMatches event |> Seq.toList

        result |> should be Empty

    [<Fact>]
    let ``given an insert record missing required fields when extracting inserted matches then nothing is returned`` () =
        let incomplete =
            makeRecord
                "INSERT"
                (dict [])
                (dict
                    [ "matchId", stringAttr matchId
                      "poolLayoutId", stringAttr poolLayoutId ])

        let event = incomplete |> List.singleton |> makeEvent

        let result = PoolLayoutEventFilter.extractInsertedMatches event |> Seq.toList

        result |> should be Empty

    [<Fact>]
    let ``given mixed inserts and modifies when extracting inserted matches then only inserts are returned`` () =
        let event =
            [ ``given an insert match record`` ()
              ``given an update record with new scores and no old scores`` ()
              ``given an update record with both old and new scores`` () ]
            |> makeEvent

        let result = PoolLayoutEventFilter.extractInsertedMatches event |> Seq.toList

        result |> should haveLength 1
