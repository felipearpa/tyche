namespace Felipearpa.Tyche.AmazonLambda.Event

open System
open Amazon.Lambda.DynamoDBEvents
open Felipearpa.Tyche.AmazonLambda
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open System.Collections.Generic
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
    let private groupNameAttributeKey = "groupName"

    [<Literal>]
    let private defaultRound = "Fase de grupos"

    [<Literal>]
    let private resultVersionAttributeKey = "resultVersion"

    /// The versioned result in an image. A result written before versioning is version 1.
    let private tryResult (image: IDictionary<string, DynamoDBEvent.AttributeValue>) =
        match
            tryGetIntFieldOrNone homeTeamScoreAttributeKey image,
            tryGetIntFieldOrNone awayTeamScoreAttributeKey image
        with
        | Some home, Some away ->
            Some
                { MatchResult.HomeTeamScore = home
                  AwayTeamScore = away
                  Version = tryGetIntFieldOrNone resultVersionAttributeKey image |> Option.defaultValue 1 }
        | _ -> None

    let private orEmpty (image: Dictionary<string, DynamoDBEvent.AttributeValue>) =
        image |> Option.ofObj |> Option.defaultWith Dictionary :> IDictionary<_, _>

    /// The newly accepted result of a modified match: a first result, or a correction with a
    /// higher result version. Score edits that keep the version are not results to settle.
    let tryResultChange (record: DynamoDBEvent.DynamodbStreamRecord) : (Ulid * MatchResult) option =
        if record.EventName <> modifyEventName then
            None
        else
            let oldImage = orEmpty record.Dynamodb.OldImage
            let newImage = orEmpty record.Dynamodb.NewImage

            match tryGetStringFieldOrNone matchIdAttributeKey newImage, tryResult oldImage, tryResult newImage with
            | Some matchId, None, Some result -> Some(Ulid.newOf matchId, result)
            | Some matchId, Some previous, Some result when result.Version > previous.Version ->
                Some(Ulid.newOf matchId, result)
            | _ -> None

    let extractUpdatedPoolLayouts (event: DynamoDBEvent) = event.Records |> Seq.choose tryResultChange

    let private tryKickoff (image: IDictionary<string, DynamoDBEvent.AttributeValue>) =
        tryGetStringFieldOrNone matchDateTimeAttributeKey image
        |> Option.map (fun stored ->
            DateTime.Parse(stored, Globalization.CultureInfo.InvariantCulture, Globalization.DateTimeStyles.RoundtripKind)
            |> KickoffGroup.instant)

    /// The previous and new kickoff of a modified match whose scheduled instant changed. A
    /// rewrite of the same instant in another format is not a change.
    let tryKickoffChange (record: DynamoDBEvent.DynamodbStreamRecord) : (Ulid * DateTime * DateTime) option =
        if record.EventName <> modifyEventName then
            None
        else
            let oldImage = orEmpty record.Dynamodb.OldImage
            let newImage = orEmpty record.Dynamodb.NewImage

            match tryGetStringFieldOrNone matchIdAttributeKey newImage, tryKickoff oldImage, tryKickoff newImage with
            | Some matchId, Some previous, Some kickoff when previous <> kickoff -> Some(Ulid.newOf matchId, previous, kickoff)
            | _ -> None

    let tryInsertedMatch (record: DynamoDBEvent.DynamodbStreamRecord) : FanOutMatchInput option =
        if record.EventName <> insertEventName then
            None
        else
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

            let groupName = tryGetStringFieldOrNone groupNameAttributeKey image

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
                      Round = round
                      GroupName = groupName }
            | _ -> None

    let extractInsertedMatches (event: DynamoDBEvent) : FanOutMatchInput seq =
        event.Records |> Seq.choose tryInsertedMatch
