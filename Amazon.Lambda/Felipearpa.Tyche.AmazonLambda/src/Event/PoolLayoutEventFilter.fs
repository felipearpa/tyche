namespace Felipearpa.Tyche.AmazonLambda.Event

open Amazon.Lambda.DynamoDBEvents
open Felipearpa.Tyche.AmazonLambda
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type

[<RequireQualifiedAccess>]
module PoolLayoutEventFilter =

    [<Literal>]
    let private updateEventName = "MODIFY"

    [<Literal>]
    let private matchIdAttributeKey = "matchId"

    [<Literal>]
    let private homeTeamScoreAttributeKey = "homeTeamScore"

    [<Literal>]
    let private awayTeamScoreAttributeKey = "awayTeamScore"

    let extractUpdatedPoolLayouts (event: DynamoDBEvent) =
        event.Records
        |> Seq.filter (fun record -> record.EventName = updateEventName)
        |> Seq.choose (fun record ->
            let oldImage = record.Dynamodb.OldImage
            let newImage = record.Dynamodb.NewImage

            let maybeMatchId = tryGetStringFieldOrNone matchIdAttributeKey newImage

            match maybeMatchId with
            | Some matchId ->
                let maybeOldHomeScore = tryGetIntFieldOrNone homeTeamScoreAttributeKey oldImage
                let maybeOldAwayScore = tryGetIntFieldOrNone awayTeamScoreAttributeKey oldImage
                let maybeNewHomeScore = tryGetIntFieldOrNone homeTeamScoreAttributeKey newImage
                let maybeNewAwayScore = tryGetIntFieldOrNone awayTeamScoreAttributeKey newImage

                match maybeOldHomeScore, maybeOldAwayScore, maybeNewHomeScore, maybeNewAwayScore with
                | None, None, Some newHomeScore, Some newAwayScore ->
                    Some
                    <| (Ulid.newOf matchId,
                        { TeamScore.HomeTeamValue = newHomeScore
                          AwayTeamValue = newAwayScore })
                | _ -> None
            | None -> None)
