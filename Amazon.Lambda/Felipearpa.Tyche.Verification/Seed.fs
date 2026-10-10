namespace Felipearpa.Tyche.Verification

#nowarn "3536"

open System
open System.Collections.Generic
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

/// Writes verification data in the record shapes described in docs/database-model.md.
module Seed =

    let s (value: string) = AttributeValue(S = value)
    let n (value: int) = AttributeValue(N = string value)

    type SeedMember =
        { GamblerId: string
          Total: int
          Position: int option }

    type SeedMatch =
        { MatchId: string
          Kickoff: DateTime
          Result: (int * int) option }

    let instant (value: DateTime) = value.ToUniversalTime().ToString("o")

    let rootItem poolId layoutId creatorId (count: int) (bound: bool) =
        let item =
            Dictionary(
                dict
                    [ "pk", s $"POOL#{poolId}"
                      "sk", s $"POOL#{poolId}"
                      "poolId", s poolId
                      "poolName", s "Verification"
                      "filter", s "Verification"
                      "poolLayoutId", s layoutId
                      "creatorGamblerId", s creatorId
                      "gamblerCount", n count ]
            )

        if bound then item["membershipRevision"] <- n count
        item

    let membershipItem poolId layoutId (seed: SeedMember) (generation: string option) =
        let item =
            Dictionary(
                dict
                    [ "pk", s $"POOL#{poolId}"
                      "sk", s $"GAMBLER#{seed.GamblerId}"
                      "poolId", s poolId
                      "poolName", s "Verification"
                      "gamblerId", s seed.GamblerId
                      "gamblerUsername", s $"user-{seed.GamblerId.Substring(18)}"
                      "gamblerEmail", s $"{seed.GamblerId.Substring(18)}@verification.invalid"
                      "status", s "OPENED"
                      "filter", s "Verification"
                      "poolLayoutId", s layoutId
                      "score", n seed.Total
                      "poolLayoutVersion", n 1
                      "getGamblersByPoolLayoutPk", s $"POOLLAYOUT#{layoutId}"
                      "getGamblersByPoolLayoutSk", s $"POOL#{poolId}#GAMBLER#{seed.GamblerId}" ]
            )

        seed.Position |> Option.iter (fun position -> item["position"] <- n position)

        generation
        |> Option.iter (fun value ->
            item["membershipGeneration"] <- s value
            item["scoreVersion"] <- n 1)

        item

    let layoutMatchItem layoutId (seed: SeedMatch) =
        let item =
            Dictionary(
                dict
                    [ "pk", s $"POOLLAYOUT#{layoutId}"
                      "sk", s $"MATCH#{seed.MatchId}"
                      "matchId", s seed.MatchId
                      "poolLayoutId", s layoutId
                      "homeTeamId", s "COL"
                      "homeTeamName", s "Colombia"
                      "awayTeamId", s "ARG"
                      "awayTeamName", s "Argentina"
                      "matchDateTime", s (instant seed.Kickoff)
                      "poolLayoutVersion", n 1
                      "round", s "Fase de grupos" ]
            )

        seed.Result
        |> Option.iter (fun (home, away) ->
            item["homeTeamScore"] <- n home
            item["awayTeamScore"] <- n away)

        item

    /// A detail; with an award it is computed before versioning; with a generation it is
    /// bound and carries an active projection of the total.
    let detailItem
        poolId
        layoutId
        (gamblerId: string)
        (seed: SeedMatch)
        (prediction: (int * int) option)
        (award: int option)
        (projection: (string * int) option)
        =
        let item =
            Dictionary(
                dict
                    [ "pk", s $"GAMBLER#{gamblerId}#POOL#{poolId}"
                      "sk", s $"MATCH#{seed.MatchId}"
                      "poolId", s poolId
                      "gamblerId", s gamblerId
                      "gamblerUsername", s $"user-{gamblerId.Substring(18)}"
                      "gamblerEmail", s $"{gamblerId.Substring(18)}@verification.invalid"
                      "matchId", s seed.MatchId
                      "homeTeamId", s "COL"
                      "homeTeamName", s "Colombia"
                      "awayTeamId", s "ARG"
                      "awayTeamName", s "Argentina"
                      "matchDateTime", s (instant seed.Kickoff)
                      "poolLayoutId", s layoutId
                      "poolLayoutVersion", n 1
                      "round", s "Fase de grupos"
                      "getPoolGamblerScoresByMatchPk", s $"MATCH#{seed.MatchId}"
                      "getPoolGamblerScoresByMatchSk", s $"POOL#{poolId}#GAMBLER#{gamblerId}" ]
            )

        prediction
        |> Option.iter (fun (home, away) ->
            item["homeTeamBet"] <- n home
            item["awayTeamBet"] <- n away)

        award
        |> Option.iter (fun value ->
            let home, away = seed.Result.Value
            item["homeTeamScore"] <- n home
            item["awayTeamScore"] <- n away
            item["score"] <- n value
            item["computedRequestId"] <- s "SEEDED"
            item["computedDateTime"] <- s (instant (seed.Kickoff.AddHours 2.0)))

        projection
        |> Option.iter (fun (generation, total) ->
            item["poolMembershipGeneration"] <- s generation
            item["poolScoreVersion"] <- n 1
            item["totalPoolPoints"] <- n total
            item["totalPoolPointsState"] <- s "KNOWN"
            item["rankingActive"] <- AttributeValue(BOOL = true)
            item["getPoolMatchBetsByTotalPk"] <- s (RankingKey.partition (Ulid.newOf poolId) (Ulid.newOf seed.MatchId))
            item["getPoolMatchBetsByTotalSk"] <- s (RankingKey.sortKey (KnownTotal total) (Ulid.newOf gamblerId))

            if award.IsSome then item["resultVersion"] <- n 1)

        item

    /// Throttled seeding requests, for the load report.
    let mutable seedingThrottles = 0

    /// Writes items with BatchWriteItem in parallel chunks, retrying unprocessed items and
    /// throttled requests with backoff.
    let writeAll (client: IAmazonDynamoDB) (tableName: string) (items: Dictionary<string, AttributeValue> seq) (parallelism: int) =
        let rec submit (requests: WriteRequest list) attempt =
            async {
                if not requests.IsEmpty then
                    if attempt > 0 then
                        do! Async.Sleep(Random.Shared.Next(50, 100 * (1 <<< min attempt 6)))

                    let! outcome =
                        client.BatchWriteItemAsync(
                            BatchWriteItemRequest(RequestItems = Dictionary(dict [ tableName, List(requests) ]))
                        )
                        |> Async.AwaitTask
                        |> Async.Catch

                    let unprocessed =
                        match outcome with
                        | Choice1Of2 response ->
                            match response.UnprocessedItems |> Option.ofObj with
                            | Some pending when pending.ContainsKey tableName -> pending[tableName] |> List.ofSeq
                            | _ -> []
                        | Choice2Of2 error when (error.GetBaseException() :? AmazonDynamoDBException) ->
                            Threading.Interlocked.Increment(&seedingThrottles) |> ignore
                            requests
                        | Choice2Of2 error -> raise error

                    if attempt > 60 && not unprocessed.IsEmpty then
                        return failwith "BatchWriteItem kept items unprocessed"
                    else
                        return! submit unprocessed (attempt + 1)
            }

        items
        |> Seq.map (fun item -> WriteRequest(PutRequest = PutRequest(Item = item)))
        |> Seq.chunkBySize 25
        |> Seq.map (fun chunk -> submit (List.ofArray chunk) 0)
        |> fun work -> Async.Parallel(work, parallelism)
        |> Async.Ignore
