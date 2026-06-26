namespace Felipearpa.Tyche.MatchScoreIngestion.Infrastructure

open System.Net.Http
open System.Text.Json
open Felipearpa.Tyche.MatchScoreIngestion.Domain

/// Reads match results from football-data.org (v4). A finished match yields its 90-minute result:
/// score.regularTime when present and non-null, otherwise score.fullTime (extra time and penalties
/// are ignored). Cancelled/postponed/awarded matches are reported as abandoned; anything else is
/// still in progress.
type FootballDataMatchResultProvider(httpClient: HttpClient) =

    [<Literal>]
    let finishedStatus = "FINISHED"

    let abandonedStatuses = set [ "POSTPONED"; "CANCELLED"; "AWARDED" ]

    let tryGetObject (element: JsonElement) (name: string) =
        match element.TryGetProperty(name) with
        | true, value when value.ValueKind <> JsonValueKind.Null -> Some value
        | _ -> None

    let tryGetFinishedResult (root: JsonElement) =
        // 90-minute rule: regularTime if present and non-null, else fullTime.
        let chosen =
            match tryGetObject root "score" with
            | Some score ->
                match tryGetObject score "regularTime" with
                | Some regularTime -> Some regularTime
                | None -> tryGetObject score "fullTime"
            | None -> None

        match chosen with
        | Some teamScore ->
            match tryGetObject teamScore "home", tryGetObject teamScore "away" with
            | Some home, Some away ->
                Some
                    { FinishedMatchResult.HomeTeamScore = home.GetInt32()
                      AwayTeamScore = away.GetInt32() }
            | _ -> None
        | None -> None

    interface IMatchResultProvider with
        member _.GetMatchPollResultAsync(externalMatchId: string) =
            async {
                let! response = httpClient.GetAsync($"v4/matches/{externalMatchId}") |> Async.AwaitTask
                response.EnsureSuccessStatusCode() |> ignore

                let! body = response.Content.ReadAsStringAsync() |> Async.AwaitTask
                use document = JsonDocument.Parse(body)
                let root = document.RootElement

                let status =
                    match root.TryGetProperty("status") with
                    | true, value -> value.GetString()
                    | _ -> ""

                if status = finishedStatus then
                    // Defensive: if the score cannot be parsed yet, keep polling instead of misreporting.
                    return
                        match tryGetFinishedResult root with
                        | Some result -> Finished result
                        | None -> NotYetFinished
                elif abandonedStatuses.Contains status then
                    return Abandoned
                else
                    return NotYetFinished
            }
