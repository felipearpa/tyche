namespace Felipearpa.Tyche.Pool.Test.Application

open System
open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Test.Fakes
open Felipearpa.Tyche.PoolLayout.Application
open Felipearpa.Tyche.PoolLayout.Infrastructure
open Felipearpa.Type
open FsUnitTyped
open Xunit

/// Kickoff groups come from the layout: every match at one UTC instant, counted as computed
/// when the layout holds its result.
module KickoffGroupReaderTest =

    let private layout = "01K1PX1TX2NM1HG851S1V0Y001"
    let private otherLayout = "01K1PX1TX2NM1HG851S1V0Y002"

    let private s (value: string) = AttributeValue(S = value)
    let private n (value: int) = AttributeValue(N = string value)

    let private layoutMatch (layoutId: string) (matchId: string) (kickoff: string) (result: (int * int) option) =
        let item =
            Dictionary(
                dict
                    [ "pk", s $"POOLLAYOUT#{layoutId}"
                      "sk", s $"MATCH#{matchId}"
                      "matchId", s matchId
                      "poolLayoutId", s layoutId
                      "homeTeamId", s "COL"
                      "homeTeamName", s "Colombia"
                      "awayTeamId", s "ARG"
                      "awayTeamName", s "Argentina"
                      "matchDateTime", s kickoff
                      "poolLayoutVersion", n 1
                      "round", s "Fase de grupos" ]
            )

        result
        |> Option.iter (fun (home, away) ->
            item["homeTeamScore"] <- n home
            item["awayTeamScore"] <- n away
            item["resultVersion"] <- n 1)

        item

    let private readerOver (fake: FakeDynamoDb) =
        let repository = PoolLayoutDynamoDbRepository(DynamoDbKeySerializer(), fake.Client)
        KickoffGroupReader(PoolLayoutMatchReader(repository))

    let private utc year month day hour =
        DateTime(year, month, day, hour, 0, 0, DateTimeKind.Utc)

    let private seeded () =
        let fake = FakeTables.create ()

        [ // One instant written with two offsets.
          layoutMatch layout "01K1PX1TX2NM1HG851S1V0M001" "2026-06-12T17:00:00Z" None
          layoutMatch layout "01K1PX1TX2NM1HG851S1V0M002" "2026-06-12T19:00:00+02:00" None
          // Later the same day.
          layoutMatch layout "01K1PX1TX2NM1HG851S1V0M003" "2026-06-12T20:00:00Z" None
          // The same 17:00 clock label at another offset is another instant.
          layoutMatch layout "01K1PX1TX2NM1HG851S1V0M004" "2026-06-12T17:00:00-05:00" None
          layoutMatch otherLayout "01K1PX1TX2NM1HG851S1V0M005" "2026-06-12T17:00:00Z" None ]
        |> List.iter (fun item -> fake.Put("PoolLayout", item))

        fake

    let private countsOf (groups: Map<DateTime, KickoffGroup>) =
        groups
        |> Map.toList
        |> List.map (fun (kickoff, group) -> kickoff, group.TotalMatches, group.ComputedMatches)

    [<Fact>]
    let ``equivalent offsets share a group and other instants of the day do not`` () =
        async {
            let fake = seeded ()
            let! groups = (readerOver fake).ReadAsync [ Ulid.newOf layout ]

            groups[Ulid.newOf layout]
            |> countsOf
            |> shouldEqual [ utc 2026 6 12 17, 2, 0; utc 2026 6 12 20, 1, 0; utc 2026 6 12 22, 1, 0 ]

            groups[Ulid.newOf layout] |> Map.forall (fun kickoff _ -> kickoff.Kind = DateTimeKind.Utc) |> shouldEqual true
        }

    [<Fact>]
    let ``a result in the layout counts as computed before the pool settles it`` () =
        async {
            let fake = seeded ()
            fake.Put("PoolLayout", layoutMatch layout "01K1PX1TX2NM1HG851S1V0M002" "2026-06-12T19:00:00+02:00" (Some(1, 0)))

            let! groups = (readerOver fake).ReadAsync [ Ulid.newOf layout ]
            let group = groups[Ulid.newOf layout] |> KickoffGroup.at (utc 2026 6 12 17)

            (group.TotalMatches, group.ComputedMatches) |> shouldEqual (2, 1)
        }

    [<Fact>]
    let ``a kickoff looked up with another offset finds its group, and one no match has is empty`` () =
        async {
            let fake = seeded ()
            let! groups = (readerOver fake).ReadAsync [ Ulid.newOf layout ]
            let groups = groups[Ulid.newOf layout]

            let offsetKickoff = DateTimeOffset(2026, 6, 12, 12, 0, 0, TimeSpan.FromHours -5.0).UtcDateTime
            (groups |> KickoffGroup.at offsetKickoff).TotalMatches |> shouldEqual 2
            (groups |> KickoffGroup.at ((utc 2026 6 12 17).ToLocalTime())).TotalMatches |> shouldEqual 2

            groups
            |> KickoffGroup.at (utc 2026 6 13 17)
            |> shouldEqual
                { KickoffDateTime = utc 2026 6 13 17
                  TotalMatches = 0
                  ComputedMatches = 0 }
        }

    [<Fact>]
    let ``each layout is read once per call across every page`` () =
        async {
            let fake = seeded ()
            fake.LimitQueryPages 1
            fake.TakeQueries() |> ignore

            let! groups =
                (readerOver fake).ReadAsync [ Ulid.newOf layout; Ulid.newOf otherLayout; Ulid.newOf layout ]

            groups.Count |> shouldEqual 2
            groups[Ulid.newOf layout] |> Map.toList |> List.sumBy (fun (_, group) -> group.TotalMatches) |> shouldEqual 4
            groups[Ulid.newOf otherLayout] |> countsOf |> shouldEqual [ utc 2026 6 12 17, 1, 0 ]

            // One traversal per distinct layout at one match per page: at most five queries for
            // the four-match layout and two for the other; a second traversal would double them.
            let queried = fake.TakeQueries() |> List.map (fun (query: QueryRequest) -> query.ExpressionAttributeValues[":pk"].S)
            queried |> List.filter ((=) $"POOLLAYOUT#{layout}") |> List.length |> shouldBeSmallerThan 6
            queried |> List.filter ((=) $"POOLLAYOUT#{otherLayout}") |> List.length |> shouldBeSmallerThan 3
        }

    [<Fact>]
    let ``no layout is read when no pool needs one`` () =
        async {
            let fake = seeded ()
            fake.TakeQueries() |> ignore
            let! groups = (readerOver fake).ReadAsync []
            groups |> shouldEqual Map.empty
            fake.TakeQueries() |> shouldEqual []
        }
