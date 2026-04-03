namespace Felipearpa.Tyche.Pool.Domain.Tests

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Type
open FsUnit.Xunit
open Xunit

module BetEvaluatorTest =

    let private makePrediction home away =
        Some
            { TeamScore.HomeTeamValue = BetScore.newOf home
              AwayTeamValue = BetScore.newOf away }

    let private makeActual home away =
        { TeamScore.HomeTeamValue = home
          AwayTeamValue = away }

    [<Fact>]
    let ``given no prediction when computing delta then zero is returned`` () =
        BetEvaluator.delta None (makeActual 2 1) |> should equal 0

    [<Fact>]
    let ``given exact score prediction when computing delta then max points are returned`` () =
        BetEvaluator.delta (makePrediction 2 1) (makeActual 2 1) |> should equal 10

    [<Fact>]
    let ``given correct winner but wrong scores when computing delta then winner points are returned`` () =
        BetEvaluator.delta (makePrediction 3 0) (makeActual 2 1) |> should equal 5

    [<Fact>]
    let ``given correct winner and home score when computing delta then winner and home points are returned`` () =
        BetEvaluator.delta (makePrediction 2 0) (makeActual 2 1) |> should equal 7

    [<Fact>]
    let ``given correct winner and away score when computing delta then winner and away points are returned`` () =
        BetEvaluator.delta (makePrediction 3 1) (makeActual 2 1) |> should equal 7

    [<Fact>]
    let ``given wrong winner when computing delta then zero is returned`` () =
        BetEvaluator.delta (makePrediction 0 2) (makeActual 2 1) |> should equal 0

    [<Fact>]
    let ``given correct draw prediction but wrong scores when computing delta then winner and difference points are returned``
        ()
        =
        BetEvaluator.delta (makePrediction 0 0) (makeActual 1 1) |> should equal 6

    [<Fact>]
    let ``given exact draw prediction when computing delta then max points are returned`` () =
        BetEvaluator.delta (makePrediction 1 1) (makeActual 1 1) |> should equal 10

    [<Fact>]
    let ``given wrong winner with correct home score when computing delta then only home points are returned`` () =
        BetEvaluator.delta (makePrediction 1 2) (makeActual 1 0) |> should equal 2

    [<Fact>]
    let ``given wrong winner with correct away score when computing delta then only away points are returned`` () =
        BetEvaluator.delta (makePrediction 0 1) (makeActual 2 1) |> should equal 2
