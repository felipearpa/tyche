namespace Felipearpa.Tyche.Pool.Domain

open System
open Felipearpa.Tyche.Pool.Type

module BetEvaluator =

    [<Literal>]
    let private pointsPerWinnerGuessing = 5

    [<Literal>]
    let private pointsPerScoreGuessing = 2

    [<Literal>]
    let private pointsPerDifferenceScoreGuessing = 1

    /// Computes the points earned by comparing a predicted score against the actual match score.
    ///
    /// | Condition                | Points |
    /// |--------------------------|--------|
    /// | Correct winner           | 5      |
    /// | Correct home team score  | 2      |
    /// | Correct away team score  | 2      |
    /// | Correct goal difference  | 1      |
    /// | No prediction            | 0      |
    ///
    /// Max: 10 (exact score prediction).
    ///
    /// Examples:
    ///
    /// | Scenario                         | Predicted | Actual | Points | Breakdown |
    /// |----------------------------------|-----------|--------|--------|-----------|
    /// | No prediction                    | None      | 2-1    | 0      | -         |
    /// | Exact score                      | 2-1       | 2-1    | 10     | 5+2+2+1   |
    /// | Correct winner, wrong scores     | 3-0       | 2-1    | 5      | 5         |
    /// | Correct winner + home score      | 2-0       | 2-1    | 7      | 5+2       |
    /// | Correct winner + away score      | 3-1       | 2-1    | 7      | 5+2       |
    /// | Wrong winner                     | 0-2       | 2-1    | 0      | -         |
    /// | Correct draw, wrong scores       | 0-0       | 1-1    | 6      | 5+1       |
    /// | Exact draw                       | 1-1       | 1-1    | 10     | 5+2+2+1   |
    /// | Wrong winner, correct home score | 1-2       | 1-0    | 2      | 2         |
    /// | Wrong winner, correct away score | 0-1       | 2-1    | 2      | 2         |
    let delta (maybePredictedScore: TeamScore<BetScore> option) (actualScore: TeamScore<int>) =
        match maybePredictedScore with
        | Some predictedScore ->
            let predictedDifference =
                predictedScore.HomeTeamValue.Value - predictedScore.AwayTeamValue.Value

            let actualDifference = actualScore.HomeTeamValue - actualScore.AwayTeamValue

            let isWinnerCorrect = Math.Sign(predictedDifference) = Math.Sign(actualDifference)

            let isHomeGoalsCorrect =
                predictedScore.HomeTeamValue.Value = actualScore.HomeTeamValue

            let isAwayGoalsCorrect =
                predictedScore.AwayTeamValue.Value = actualScore.AwayTeamValue

            let isGoalDifferenceCorrect = predictedDifference = actualDifference

            (if isWinnerCorrect then pointsPerWinnerGuessing else 0)
            + (if isHomeGoalsCorrect then pointsPerScoreGuessing else 0)
            + (if isAwayGoalsCorrect then pointsPerScoreGuessing else 0)
            + (if isGoalDifferenceCorrect then
                   pointsPerDifferenceScoreGuessing
               else
                   0)
        | None -> 0
