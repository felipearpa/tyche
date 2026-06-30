namespace Felipearpa.Tyche.Pool.Domain

open System
open Felipearpa.Tyche.Pool.Type

module BetEvaluator =

    [<Literal>]
    let private pointsPerWinnerGuessing = 10

    [<Literal>]
    let private pointsPerScoreGuessing = 4

    [<Literal>]
    let private pointsPerDifferenceScoreGuessing = 2

    /// Computes the points earned by comparing a predicted score against the actual match score.
    ///
    /// | Condition                       | Points |
    /// |---------------------------------|--------|
    /// | Correct winner                  | 10     |
    /// | Correct home team score         | 4      |
    /// | Correct away team score         | 4      |
    /// | Correct goal difference (magnitude) | 2  |
    /// | No prediction                   | 0      |
    ///
    /// The goal-difference point rewards the absolute margin between the teams, so it is
    /// awarded even when the winner is reversed (e.g. predicting 1-2 for an actual 2-1
    /// still matches the one-goal margin).
    ///
    /// Max: 20 (exact score prediction).
    ///
    /// Examples:
    ///
    /// | Scenario                         | Predicted | Actual | Points | Breakdown  |
    /// |----------------------------------|-----------|--------|--------|------------|
    /// | No prediction                    | None      | 2-1    | 0      | -          |
    /// | Exact score                      | 2-1       | 2-1    | 20     | 10+4+4+2   |
    /// | Correct winner, wrong scores     | 3-0       | 2-1    | 10     | 10         |
    /// | Correct winner + home score      | 2-0       | 2-1    | 14     | 10+4       |
    /// | Correct winner + away score      | 3-1       | 2-1    | 14     | 10+4       |
    /// | Wrong winner                     | 0-2       | 2-1    | 0      | -          |
    /// | Reversed score, same margin      | 1-2       | 2-1    | 2      | 2          |
    /// | Correct draw, wrong scores       | 0-0       | 1-1    | 12     | 10+2       |
    /// | Exact draw                       | 1-1       | 1-1    | 20     | 10+4+4+2   |
    /// | Wrong winner, correct home score | 1-2       | 1-0    | 6      | 4+2        |
    /// | Wrong winner, correct away score | 0-1       | 2-1    | 6      | 4+2        |
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

            let isGoalDifferenceCorrect = abs predictedDifference = abs actualDifference

            (if isWinnerCorrect then pointsPerWinnerGuessing else 0)
            + (if isHomeGoalsCorrect then pointsPerScoreGuessing else 0)
            + (if isAwayGoalsCorrect then pointsPerScoreGuessing else 0)
            + (if isGoalDifferenceCorrect then
                   pointsPerDifferenceScoreGuessing
               else
                   0)
        | None -> 0
