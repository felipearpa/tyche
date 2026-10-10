namespace Felipearpa.Tyche.Pool.Test.Domain

open System
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type
open FsUnitTyped
open Xunit

module RankingKeyTest =

    let private gamblerA = Ulid.newOf "01K1PX1TX2NM1HG851S1V0GA01"
    let private gamblerB = Ulid.newOf "01K1PX1TX2NM1HG851S1V0GB01"

    let private ascending (keys: string list) =
        keys |> List.sortWith (fun l r -> String.CompareOrdinal(l, r))

    [<Fact>]
    let ``known totals sort descending across the whole Int32 domain`` () =
        let totals = [ Int32.MinValue; -1; 0; 1; 9; 10; 99; 100; 120; Int32.MaxValue ]
        let keys = totals |> List.map (fun total -> RankingKey.sortKey (KnownTotal total) gamblerA, total)

        keys
        |> List.sortWith (fun (l, _) (r, _) -> String.CompareOrdinal(l, r))
        |> List.map snd
        |> shouldEqual (totals |> List.sortDescending)

    [<Fact>]
    let ``keys have a fixed width so lexical order is numeric order`` () =
        [ Int32.MinValue; 0; 7; Int32.MaxValue ]
        |> List.map (fun total -> (RankingKey.sortKey (KnownTotal total) gamblerA).Length)
        |> List.distinct
        |> shouldEqual [ 41 ]

    [<Fact>]
    let ``equal totals break ties by gambler ID ascending, including an all-zero pool`` () =
        [ RankingKey.sortKey (KnownTotal 0) gamblerB; RankingKey.sortKey (KnownTotal 0) gamblerA ]
        |> ascending
        |> shouldEqual [ RankingKey.sortKey (KnownTotal 0) gamblerA; RankingKey.sortKey (KnownTotal 0) gamblerB ]

    [<Fact>]
    let ``confirmed zero and the lowest known total precede every unavailable total`` () =
        let unavailableFirstGambler = RankingKey.sortKey UnavailableTotal gamblerA
        let zero = RankingKey.sortKey (KnownTotal 0) gamblerB
        let lowest = RankingKey.sortKey (KnownTotal Int32.MinValue) gamblerB

        [ unavailableFirstGambler; zero; lowest ] |> ascending |> shouldEqual [ zero; lowest; unavailableFirstGambler ]

    [<Fact>]
    let ``unavailable totals keep gambler ID order and carry the schema version`` () =
        let first = RankingKey.sortKey UnavailableTotal gamblerA
        let second = RankingKey.sortKey UnavailableTotal gamblerB

        [ second; first ] |> ascending |> shouldEqual [ first; second ]
        RankingKey.trySchemaVersion first |> shouldEqual (Some RankingKey.schemaVersion)
        RankingKey.trySchemaVersion (RankingKey.sortKey (KnownTotal 5) gamblerA) |> shouldEqual (Some 1)

    [<Fact>]
    let ``the partition key is per pool and match`` () =
        RankingKey.partition (Ulid.newOf "01K1PX1TX2NM1HG851S1V0P001") (Ulid.newOf "01K1PX1TX2NM1HG851S1V0M001")
        |> shouldEqual "POOL#01K1PX1TX2NM1HG851S1V0P001#MATCH#01K1PX1TX2NM1HG851S1V0M001"
