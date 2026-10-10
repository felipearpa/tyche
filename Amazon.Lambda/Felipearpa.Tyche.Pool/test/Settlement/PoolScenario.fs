namespace Felipearpa.Tyche.Pool.Test.Settlement

open System
open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Pool.Test.Fakes

/// A pool seeded in the legacy (pre-versioning) record shape, plus helpers that read the
/// persisted outcome back from the fake table.
module PoolScenario =

    let poolId = "01K1PX1TX2NM1HG851S1V0P001"
    let poolLayoutId = "01K1PX1TX2NM1HG851S1V0Y001"
    let gamblerA = "01K1PX1TX2NM1HG851S1V0GA01"
    let gamblerB = "01K1PX1TX2NM1HG851S1V0GB01"
    let gamblerC = "01K1PX1TX2NM1HG851S1V0GC01"
    let gamblerD = "01K1PX1TX2NM1HG851S1V0GD01"
    let match0 = "01K1PX1TX2NM1HG851S1V0M000"
    let match1 = "01K1PX1TX2NM1HG851S1V0M001"
    let match2 = "01K1PX1TX2NM1HG851S1V0M002"
    let match3 = "01K1PX1TX2NM1HG851S1V0M003"

    type MemberSeed =
        { GamblerId: string
          Total: int
          Position: int option
          Predictions: (string * (int * int)) list }

    type MatchSeed =
        { MatchId: string
          MatchDateTime: DateTime
          Result: (int * int) option }

    let private s (value: string) = AttributeValue(S = value)
    let private n (value: int) = AttributeValue(N = string value)

    let rootItem (gamblerCount: int) =
        dict
            [ "pk", s $"POOL#{poolId}"
              "sk", s $"POOL#{poolId}"
              "poolId", s poolId
              "poolName", s "Polla"
              "filter", s "Polla"
              "poolLayoutId", s poolLayoutId
              "creatorGamblerId", s gamblerA
              "gamblerCount", n gamblerCount ]

    let membershipItem (seed: MemberSeed) =
        let item =
            Dictionary(
                dict
                    [ "pk", s $"POOL#{poolId}"
                      "sk", s $"GAMBLER#{seed.GamblerId}"
                      "poolId", s poolId
                      "poolName", s "Polla"
                      "gamblerId", s seed.GamblerId
                      "gamblerUsername", s $"user-{seed.GamblerId.Substring(22)}"
                      "gamblerEmail", s $"{seed.GamblerId.Substring(22)}@example.com"
                      "status", s "OPENED"
                      "filter", s "Polla"
                      "poolLayoutId", s poolLayoutId
                      "score", n seed.Total
                      "poolLayoutVersion", n 1
                      "getGamblersByPoolLayoutPk", s $"POOLLAYOUT#{poolLayoutId}"
                      "getGamblersByPoolLayoutSk", s $"POOL#{poolId}#GAMBLER#{seed.GamblerId}" ]
            )

        seed.Position |> Option.iter (fun position -> item["position"] <- n position)
        item

    let layoutMatchItem (seed: MatchSeed) =
        let item =
            Dictionary(
                dict
                    [ "pk", s $"POOLLAYOUT#{poolLayoutId}"
                      "sk", s $"MATCH#{seed.MatchId}"
                      "matchId", s seed.MatchId
                      "poolLayoutId", s poolLayoutId
                      "homeTeamId", s "COL"
                      "homeTeamName", s "Colombia"
                      "awayTeamId", s "ARG"
                      "awayTeamName", s "Argentina"
                      "matchDateTime", s (seed.MatchDateTime.ToUniversalTime().ToString("o"))
                      "poolLayoutVersion", n 1
                      "round", s "Fase de grupos" ]
            )

        seed.Result
        |> Option.iter (fun (home, away) ->
            item["homeTeamScore"] <- n home
            item["awayTeamScore"] <- n away)

        item

    let detailItem (gamblerId: string) (seed: MatchSeed) (prediction: (int * int) option) =
        let item =
            Dictionary(
                dict
                    [ "pk", s $"GAMBLER#{gamblerId}#POOL#{poolId}"
                      "sk", s $"MATCH#{seed.MatchId}"
                      "poolId", s poolId
                      "gamblerId", s gamblerId
                      "gamblerUsername", s $"user-{gamblerId.Substring(22)}"
                      "gamblerEmail", s $"{gamblerId.Substring(22)}@example.com"
                      "matchId", s seed.MatchId
                      "homeTeamId", s "COL"
                      "homeTeamName", s "Colombia"
                      "awayTeamId", s "ARG"
                      "awayTeamName", s "Argentina"
                      "matchDateTime", s (seed.MatchDateTime.ToUniversalTime().ToString("o"))
                      "poolLayoutId", s poolLayoutId
                      "poolLayoutVersion", n 1
                      "round", s "Fase de grupos"
                      "getPoolGamblerScoresByMatchPk", s $"MATCH#{seed.MatchId}"
                      "getPoolGamblerScoresByMatchSk", s $"POOL#{poolId}#GAMBLER#{gamblerId}" ]
            )

        prediction
        |> Option.iter (fun (home, away) ->
            item["homeTeamBet"] <- n home
            item["awayTeamBet"] <- n away)

        item

    /// A detail scored before result versions existed: computed, with an award and no version.
    let legacyComputedDetail (gamblerId: string) (seed: MatchSeed) (prediction: (int * int) option) (award: int) =
        let item = detailItem gamblerId seed prediction
        let home, away = seed.Result.Value
        item["homeTeamScore"] <- n home
        item["awayTeamScore"] <- n away
        item["score"] <- n award
        item["computedRequestId"] <- s "01K1PX1TX2NM1HG851S1V0R000"
        item["computedDateTime"] <- s (seed.MatchDateTime.AddHours(2.0).ToUniversalTime().ToString("o"))
        item

    let pastKickoff = DateTime.UtcNow.AddHours(-3.0)

    /// Already settled before the scenarios start; it produced every member's current total.
    let historicalMatch =
        { MatchId = match0
          MatchDateTime = pastKickoff.AddDays(-2.0)
          Result = Some(2, 1) }

    let defaultMatches =
        [ { MatchId = match1
            MatchDateTime = pastKickoff
            Result = None }
          { MatchId = match2
            MatchDateTime = pastKickoff.AddMinutes(30.0)
            Result = None } ]

    /// A has 4 points (3rd), B leads with 10, C has 6 (2nd); each total comes from the
    /// historical match 0, which ended 2-1 (A predicted 2-5: +4, B 3-0: +10, C 2-3: +6).
    let defaultMembers =
        [ { GamblerId = gamblerA
            Total = 4
            Position = Some 3
            Predictions = [ match0, (2, 5); match1, (1, 0); match2, (0, 0) ] }
          { GamblerId = gamblerB
            Total = 10
            Position = Some 1
            Predictions = [ match0, (3, 0); match1, (2, 2); match2, (1, 0) ] }
          { GamblerId = gamblerC
            Total = 6
            Position = Some 2
            Predictions = [ match0, (2, 3); match2, (3, 1) ] } ]

    let private predictionFor (memberSeed: MemberSeed) (matchId: string) =
        memberSeed.Predictions
        |> List.tryFind (fun (predicted, _) -> predicted = matchId)
        |> Option.map snd

    /// Writes the legacy record shapes: root, memberships, layout matches, the computed
    /// historical match, and uncomputed details for the given matches.
    let seedLegacy (fake: FakeDynamoDb) (members: MemberSeed list) (matches: MatchSeed list) =
        fake.Put("Pool", rootItem members.Length)
        fake.Put("PoolLayout", layoutMatchItem historicalMatch)

        for matchSeed in matches do
            fake.Put("PoolLayout", layoutMatchItem matchSeed)

        for memberSeed in members do
            fake.Put("Pool", membershipItem memberSeed)

            fake.Put(
                "Pool",
                legacyComputedDetail memberSeed.GamblerId historicalMatch (predictionFor memberSeed match0) memberSeed.Total
            )

            for matchSeed in matches do
                fake.Put("Pool", detailItem memberSeed.GamblerId matchSeed (predictionFor memberSeed matchSeed.MatchId))

    let private key (pk: string) (sk: string) = dict [ "pk", s pk; "sk", s sk ]

    let membership (fake: FakeDynamoDb) (gamblerId: string) =
        fake.TryGet("Pool", key $"POOL#{poolId}" $"GAMBLER#{gamblerId}")

    let detail (fake: FakeDynamoDb) (gamblerId: string) (matchId: string) =
        fake.TryGet("Pool", key $"GAMBLER#{gamblerId}#POOL#{poolId}" $"MATCH#{matchId}")

    let root (fake: FakeDynamoDb) = fake.TryGet("Pool", key $"POOL#{poolId}" $"POOL#{poolId}")

    let number (attribute: string) (item: Dictionary<string, AttributeValue> option) =
        item
        |> Option.bind (fun found ->
            match found.TryGetValue attribute with
            | true, value when not (isNull value.N) -> Some(int value.N)
            | _ -> None)

    let text (attribute: string) (item: Dictionary<string, AttributeValue> option) =
        item
        |> Option.bind (fun found ->
            match found.TryGetValue attribute with
            | true, value when not (isNull value.S) -> Some value.S
            | _ -> None)

    let has (attribute: string) (item: Dictionary<string, AttributeValue> option) =
        item |> Option.exists (fun found -> found.ContainsKey attribute)

    let total fake gamblerId = membership fake gamblerId |> number "score"
    let position fake gamblerId = membership fake gamblerId |> number "position"
    let beforePosition fake gamblerId = membership fake gamblerId |> number "beforePosition"

    let award fake gamblerId matchId = detail fake gamblerId matchId |> number "score"
    let isComputed fake gamblerId matchId = detail fake gamblerId matchId |> has "computedRequestId"

    let detailPosition fake gamblerId matchId =
        detail fake gamblerId matchId |> number "position"

    let detailBeforePosition fake gamblerId matchId =
        detail fake gamblerId matchId |> number "beforePosition"
