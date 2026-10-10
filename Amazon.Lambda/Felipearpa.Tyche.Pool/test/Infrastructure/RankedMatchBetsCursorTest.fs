namespace Felipearpa.Tyche.Pool.Test.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Type
open FsUnitTyped
open Xunit

module RankedMatchBetsCursorTest =

    let private poolId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0P001"
    let private otherPool = Ulid.newOf "01K1PX1TX2NM1HG851S1V0P002"
    let private matchId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0M001"
    let private otherMatch = Ulid.newOf "01K1PX1TX2NM1HG851S1V0M002"
    let private gamblerId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0GA01"

    let private lastEvaluatedKey (pool: Ulid) (matchValue: Ulid) =
        dict
            [ "pk", AttributeValue(S = $"GAMBLER#{gamblerId.Value}#POOL#{pool.Value}")
              "sk", AttributeValue(S = $"MATCH#{matchValue.Value}")
              "getPoolMatchBetsByTotalPk", AttributeValue(S = RankingKey.partition pool matchValue)
              "getPoolMatchBetsByTotalSk", AttributeValue(S = RankingKey.sortKey (KnownTotal 0) gamblerId) ]

    [<Fact>]
    let ``a cursor resumes from the exact native start key of its context`` () =
        let cursor = RankedMatchBetsCursor.encode poolId matchId (lastEvaluatedKey poolId matchId)

        match RankedMatchBetsCursor.tryDecode poolId matchId cursor with
        | Ok start ->
            start["getPoolMatchBetsByTotalSk"].S |> shouldEqual (RankingKey.sortKey (KnownTotal 0) gamblerId)
            start["getPoolMatchBetsByTotalPk"].S |> shouldEqual (RankingKey.partition poolId matchId)
            start["pk"].S |> shouldEqual $"GAMBLER#{gamblerId.Value}#POOL#{poolId.Value}"
        | Error error -> failwith $"unexpected {error}"

    [<Fact>]
    let ``a cursor from another pool or match is rejected`` () =
        let cursor = RankedMatchBetsCursor.encode poolId matchId (lastEvaluatedKey poolId matchId)

        RankedMatchBetsCursor.tryDecode otherPool matchId cursor |> shouldEqual (Error CursorContextMismatch)
        RankedMatchBetsCursor.tryDecode poolId otherMatch cursor |> shouldEqual (Error CursorContextMismatch)

    [<Fact>]
    let ``malformed and legacy cursors are rejected`` () =
        RankedMatchBetsCursor.tryDecode poolId matchId "not-a-cursor" |> shouldEqual (Error MalformedCursor)

        // The legacy match-bets mode serializes only pk and sk.
        RankedMatchBetsCursor.tryDecode poolId matchId """{"pk":"x","sk":"y"}""" |> shouldEqual (Error MalformedCursor)

    [<Fact>]
    let ``a cursor whose start key points outside the requested context is rejected`` () =
        let forged = RankedMatchBetsCursor.encode poolId matchId (lastEvaluatedKey otherPool matchId)
        RankedMatchBetsCursor.tryDecode poolId matchId forged |> shouldEqual (Error MalformedCursor)
