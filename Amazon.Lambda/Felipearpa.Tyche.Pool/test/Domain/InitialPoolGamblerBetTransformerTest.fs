namespace Felipearpa.Tyche.Pool.Test.Domain

open System
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Domain.InitialPoolGamblerBetTransformer
open Felipearpa.Type
open FsUnitTyped
open Xunit

module InitialPoolGamblerBetTransformerTest =

    let private bet () : InitialPoolGamblerBet =
        { PoolId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6N"
          GamblerId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6P"
          GamblerUsername = NonEmptyString100.newOf "felipe@tyche.com"
          MatchId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6Q"
          PoolLayoutId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6R"
          HomeTeamId = "01K1PX1TX2NM1HG851S1V0QG6S"
          HomeTeamName = NonEmptyString100.newOf "Tigre"
          AwayTeamId = "01K1PX1TX2NM1HG851S1V0QG6T"
          AwayTeamName = NonEmptyString100.newOf "América"
          MatchDateTime = DateTime(2026, 5, 1, 0, 0, 0, DateTimeKind.Utc)
          PoolLayoutVersion = 2
          Round = "Fase de grupos"
          GroupName = None
          HomeTeamScore = None
          AwayTeamScore = None
          BetScore = None
          ComputedDateTime = None
          ComputedRequestId = None }

    [<Fact>]
    let ``given a pending bet when transformed then optional fields are absent`` () =
        let item = bet () |> toAmazonItem

        item.ContainsKey("score") |> shouldEqual false
        item.ContainsKey("computedDateTime") |> shouldEqual false
        item.ContainsKey("computedRequestId") |> shouldEqual false
        item.ContainsKey("homeTeamScore") |> shouldEqual false
        item.ContainsKey("awayTeamScore") |> shouldEqual false
        item.ContainsKey("groupName") |> shouldEqual false

    [<Fact>]
    let ``given a bet when transformed then gamblerEmail mirrors gamblerUsername`` () =
        let item = bet () |> toAmazonItem

        item["gamblerEmail"].S |> shouldEqual "felipe@tyche.com"
        item["gamblerUsername"].S |> shouldEqual "felipe@tyche.com"

    [<Fact>]
    let ``given a bet with a group name when transformed then groupName is written`` () =
        let item =
            { bet () with
                GroupName = Some "Group A" }
            |> toAmazonItem

        item["groupName"].S |> shouldEqual "Group A"

    [<Fact>]
    let ``given a bet when transformed then round is written`` () =
        let item =
            { bet () with
                Round = "Octavos de final" }
            |> toAmazonItem

        item["round"].S |> shouldEqual "Octavos de final"

    [<Fact>]
    let ``given a backfilled bet for an already-scored match when transformed then all optional fields are present`` () =
        let computedAt = DateTime(2026, 5, 1, 0, 0, 0, DateTimeKind.Utc)

        let backfilled =
            { bet () with
                HomeTeamScore = Some 2
                AwayTeamScore = Some 1
                BetScore = Some 0
                ComputedDateTime = Some computedAt
                ComputedRequestId = Some "01K1PX1TX2NM1HG851S1V0QG6Z" }

        let item = backfilled |> toAmazonItem

        item["score"].N |> shouldEqual "0"
        item["homeTeamScore"].N |> shouldEqual "2"
        item["awayTeamScore"].N |> shouldEqual "1"
        item["computedRequestId"].S |> shouldEqual "01K1PX1TX2NM1HG851S1V0QG6Z"
        item["computedDateTime"].S |> shouldEqual (computedAt.ToString("o"))
