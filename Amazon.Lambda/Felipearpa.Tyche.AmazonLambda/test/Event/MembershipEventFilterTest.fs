namespace Felipearpa.Tyche.AmazonLambda.Event.Tests

open System.Collections.Generic
open Amazon.Lambda.DynamoDBEvents
open Felipearpa.Tyche.AmazonLambda.Event
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Type
open FsUnit.Xunit
open Xunit

module MembershipEventFilterTest =

    let private poolId = "01K1PX1TX2NM1HG851S1V0P001"
    let private gamblerId = "01K1PX1TX2NM1HG851S1V0GA01"

    let private attr value = DynamoDBEvent.AttributeValue(S = value)
    let private num (value: int) = DynamoDBEvent.AttributeValue(N = string value)

    let private record eventName (sk: string) (oldImage: (string * DynamoDBEvent.AttributeValue) list) (newImage: (string * DynamoDBEvent.AttributeValue) list) =
        let stream = DynamoDBEvent.StreamRecord()
        stream.Keys <- Dictionary(dict [ "pk", attr $"POOL#{poolId}"; "sk", attr sk ])
        stream.OldImage <- Dictionary(dict oldImage)
        stream.NewImage <- Dictionary(dict newImage)
        let result = DynamoDBEvent.DynamodbStreamRecord()
        result.EventName <- eventName
        result.Dynamodb <- stream
        result

    let private membershipKey = $"GAMBLER#{gamblerId}"

    [<Fact>]
    let ``joins and removals request standings reconciliation, and joins carry their generation`` () =
        MembershipEventFilter.tryChange (record "INSERT" membershipKey [] [ "membershipGeneration", attr "G2" ])
        |> should equal (Some(MembershipCreated(Ulid.newOf poolId, Ulid.newOf gamblerId, Some "G2")))

        MembershipEventFilter.tryChange (record "INSERT" membershipKey [] [])
        |> should equal (Some(MembershipCreated(Ulid.newOf poolId, Ulid.newOf gamblerId, None)))

        MembershipEventFilter.tryChange (record "REMOVE" membershipKey [] [])
        |> should equal (Some(MembershipRemoved(Ulid.newOf poolId, Ulid.newOf gamblerId)))

    [<Fact>]
    let ``a new score version requests propagation`` () =
        MembershipEventFilter.tryChange (record "MODIFY" membershipKey [ "scoreVersion", num 1 ] [ "scoreVersion", num 2 ])
        |> should equal (Some(MembershipTotalChanged(Ulid.newOf poolId, Ulid.newOf gamblerId)))

    [<Fact>]
    let ``position and before-score writes do not trigger propagation`` () =
        MembershipEventFilter.tryChange (
            record
                "MODIFY"
                membershipKey
                [ "scoreVersion", num 2; "membershipGeneration", attr "G"; "position", num 3 ]
                [ "scoreVersion", num 2; "membershipGeneration", attr "G"; "position", num 1 ]
        )
        |> should equal None

    [<Fact>]
    let ``receipts, the pool root, and details are not memberships`` () =
        MembershipEventFilter.tryChange (record "INSERT" "SETTLEMENT#RESULT#X#1" [] []) |> should equal None
        MembershipEventFilter.tryChange (record "MODIFY" $"POOL#{poolId}" [] []) |> should equal None
