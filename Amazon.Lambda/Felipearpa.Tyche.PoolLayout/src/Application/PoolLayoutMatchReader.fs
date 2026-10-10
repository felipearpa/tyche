namespace Felipearpa.Tyche.PoolLayout.Application

open Felipearpa.Core.Paging
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.PoolLayout.Domain

/// Gives the Pool context strongly consistent layout matches with their versioned results.
type PoolLayoutMatchReader(poolLayoutRepository: IPoolLayoutRepository) =

    let toSnapshot (layoutMatch: PoolLayoutMatch) : PoolLayoutMatchSnapshot =
        { MatchId = layoutMatch.MatchId
          PoolLayoutId = layoutMatch.PoolLayoutId
          PoolLayoutVersion = layoutMatch.PoolLayoutVersion
          HomeTeamId = layoutMatch.HomeTeamId
          HomeTeamName = layoutMatch.HomeTeamName
          AwayTeamId = layoutMatch.AwayTeamId
          AwayTeamName = layoutMatch.AwayTeamName
          MatchDateTime = layoutMatch.MatchDateTime
          Round = layoutMatch.Round
          GroupName = layoutMatch.GroupName
          Result =
            match layoutMatch.HomeTeamScore, layoutMatch.AwayTeamScore, layoutMatch.ResultVersion with
            | Some home, Some away, Some version ->
                Some
                    { HomeTeamScore = home
                      AwayTeamScore = away
                      Version = version }
            | _ -> None }

    interface IPoolLayoutMatchReader with

        member this.GetMatchAsync(poolLayoutId, matchId) =
            async {
                let! layoutMatch = poolLayoutRepository.GetMatchAsync(poolLayoutId, matchId)
                return layoutMatch |> Option.map toSnapshot
            }

        member this.GetMatchesThroughVersionAsync(poolLayoutId, poolLayoutVersion, next) =
            async {
                let! page = poolLayoutRepository.GetPoolLayoutMatchesThroughVersionAsync(poolLayoutId, poolLayoutVersion, next)
                return page |> CursorPage.map toSnapshot
            }
