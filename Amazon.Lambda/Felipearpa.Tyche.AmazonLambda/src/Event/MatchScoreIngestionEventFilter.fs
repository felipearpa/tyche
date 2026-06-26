namespace Felipearpa.Tyche.AmazonLambda.Event

open System
open Amazon.Lambda.DynamoDBEvents
open Felipearpa.Tyche.AmazonLambda
open Felipearpa.Tyche.MatchScoreIngestion.Domain
open Felipearpa.Type

[<RequireQualifiedAccess>]
module MatchScoreIngestionEventFilter =

    [<Literal>]
    let private insertEventName = "INSERT"

    [<Literal>]
    let private modifyEventName = "MODIFY"

    [<Literal>]
    let private removeEventName = "REMOVE"

    [<Literal>]
    let private matchIdKey = "matchId"

    [<Literal>]
    let private poolLayoutIdKey = "poolLayoutId"

    [<Literal>]
    let private externalMatchIdKey = "externalMatchId"

    [<Literal>]
    let private matchDateTimeKey = "matchDateTime"

    [<Literal>]
    let private statusKey = "status"

    [<Literal>]
    let private completedStatus = "COMPLETED"

    [<Literal>]
    let private expiredStatus = "EXPIRED"

    // A row is terminal once it is completed or expired; either way its schedule must be torn down.
    let private isTerminal (status: string option) =
        status = Some completedStatus || status = Some expiredStatus

    /// New control rows → a schedule to create.
    let extractSchedulesToCreate (event: DynamoDBEvent) : MatchScoreSchedule seq =
        event.Records
        |> Seq.filter (fun record -> record.EventName = insertEventName)
        |> Seq.choose (fun record ->
            let image = record.Dynamodb.NewImage

            match
                tryGetStringFieldOrNone matchIdKey image,
                tryGetStringFieldOrNone poolLayoutIdKey image,
                tryGetStringFieldOrNone externalMatchIdKey image,
                tryGetStringFieldOrNone matchDateTimeKey image
            with
            | Some matchId, Some poolLayoutId, Some externalMatchId, Some matchDateTime ->
                Some
                    { MatchScoreSchedule.MatchId = Ulid.newOf matchId
                      PoolLayoutId = Ulid.newOf poolLayoutId
                      ExternalMatchId = externalMatchId
                      MatchDateTime = DateTime.Parse(matchDateTime) }
            | _ -> None)

    /// Removed rows (manual cancellation) and rows transitioning to COMPLETED → a schedule to delete.
    let extractSchedulesToDelete (event: DynamoDBEvent) : Ulid seq =
        event.Records
        |> Seq.choose (fun record ->
            match record.EventName with
            | name when name = removeEventName ->
                tryGetStringFieldOrNone matchIdKey record.Dynamodb.OldImage
                |> Option.map Ulid.newOf
            | name when name = modifyEventName ->
                let oldStatus = tryGetStringFieldOrNone statusKey record.Dynamodb.OldImage
                let newStatus = tryGetStringFieldOrNone statusKey record.Dynamodb.NewImage

                // Only the transition into a terminal status tears down the schedule; poll-tick
                // modifies (status stays PENDING) and already-terminal rows are ignored.
                if isTerminal newStatus && not (isTerminal oldStatus) then
                    tryGetStringFieldOrNone matchIdKey record.Dynamodb.NewImage
                    |> Option.map Ulid.newOf
                else
                    None
            | _ -> None)
