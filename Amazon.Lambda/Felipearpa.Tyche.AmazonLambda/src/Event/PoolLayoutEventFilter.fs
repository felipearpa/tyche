namespace Felipearpa.Tyche.AmazonLambda.Event

open System
open Amazon.Lambda.DynamoDBEvents
open Felipearpa.Tyche.AmazonLambda
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type

[<RequireQualifiedAccess>]
module PoolLayoutEventFilter =

    [<Literal>]
    let private modifyEventName = "MODIFY"

    [<Literal>]
    let private insertEventName = "INSERT"

    [<Literal>]
    let private matchIdAttributeKey = "matchId"

    [<Literal>]
    let private poolLayoutIdAttributeKey = "poolLayoutId"

    [<Literal>]
    let private poolLayoutVersionAttributeKey = "poolLayoutVersion"

    [<Literal>]
    let private homeTeamScoreAttributeKey = "homeTeamScore"

    [<Literal>]
    let private awayTeamScoreAttributeKey = "awayTeamScore"

    [<Literal>]
    let private homeTeamIdAttributeKey = "homeTeamId"

    [<Literal>]
    let private homeTeamNameAttributeKey = "homeTeamName"

    [<Literal>]
    let private awayTeamIdAttributeKey = "awayTeamId"

    [<Literal>]
    let private awayTeamNameAttributeKey = "awayTeamName"

    [<Literal>]
    let private matchDateTimeAttributeKey = "matchDateTime"

    [<Literal>]
    let private roundAttributeKey = "round"

    [<Literal>]
    let private defaultRound = "Fase de grupos"

    let extractUpdatedPoolLayouts (event: DynamoDBEvent) =
        event.Records
        |> Seq.filter (fun record -> record.EventName = modifyEventName)
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

    let extractInsertedMatches (event: DynamoDBEvent) : FanOutMatchInput seq =
        event.Records
        |> Seq.filter (fun record -> record.EventName = insertEventName)
        |> Seq.choose (fun record ->
            let image = record.Dynamodb.NewImage

            let maybeMatchId = tryGetStringFieldOrNone matchIdAttributeKey image
            let maybePoolLayoutId = tryGetStringFieldOrNone poolLayoutIdAttributeKey image
            let maybePoolLayoutVersion = tryGetIntFieldOrNone poolLayoutVersionAttributeKey image
            let maybeHomeTeamId = tryGetStringFieldOrNone homeTeamIdAttributeKey image
            let maybeHomeTeamName = tryGetStringFieldOrNone homeTeamNameAttributeKey image
            let maybeAwayTeamId = tryGetStringFieldOrNone awayTeamIdAttributeKey image
            let maybeAwayTeamName = tryGetStringFieldOrNone awayTeamNameAttributeKey image
            let maybeMatchDateTime = tryGetStringFieldOrNone matchDateTimeAttributeKey image

            let round =
                tryGetStringFieldOrNone roundAttributeKey image
                |> Option.defaultValue defaultRound

            match
                maybeMatchId,
                maybePoolLayoutId,
                maybePoolLayoutVersion,
                maybeHomeTeamId,
                maybeHomeTeamName,
                maybeAwayTeamId,
                maybeAwayTeamName,
                maybeMatchDateTime
            with
            | Some matchId,
              Some poolLayoutId,
              Some poolLayoutVersion,
              Some homeTeamId,
              Some homeTeamName,
              Some awayTeamId,
              Some awayTeamName,
              Some matchDateTime ->
                Some
                    { FanOutMatchInput.MatchId = Ulid.newOf matchId
                      PoolLayoutId = Ulid.newOf poolLayoutId
                      PoolLayoutVersion = poolLayoutVersion
                      HomeTeamId = homeTeamId
                      HomeTeamName = NonEmptyString100.newOf homeTeamName
                      AwayTeamId = awayTeamId
                      AwayTeamName = NonEmptyString100.newOf awayTeamName
                      MatchDateTime = DateTime.Parse(matchDateTime)
                      Round = round }
            | _ -> None)
