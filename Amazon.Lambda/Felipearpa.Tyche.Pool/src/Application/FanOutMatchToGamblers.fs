namespace Felipearpa.Tyche.Pool.Application

open System
open Felipearpa.Core
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type FanOutMatchInput =
    { MatchId: Ulid
      PoolLayoutId: Ulid
      PoolLayoutVersion: int
      HomeTeamId: string
      HomeTeamName: NonEmptyString100
      AwayTeamId: string
      AwayTeamName: NonEmptyString100
      MatchDateTime: DateTime
      Round: string }

type FanOutMatchToGamblers
    (poolRepository: IPoolRepository, poolGamblerBetRepository: IPoolGamblerBetRepository) =

    member this.ExecuteAsync(input: FanOutMatchInput) : Async<unit> =
        let buildBet (gambler: PoolLayoutGambler) =
            { InitialPoolGamblerBet.PoolId = gambler.PoolId
              GamblerId = gambler.GamblerId
              GamblerUsername = gambler.GamblerUsername
              MatchId = input.MatchId
              PoolLayoutId = input.PoolLayoutId
              HomeTeamId = input.HomeTeamId
              HomeTeamName = input.HomeTeamName
              AwayTeamId = input.AwayTeamId
              AwayTeamName = input.AwayTeamName
              MatchDateTime = input.MatchDateTime
              PoolLayoutVersion = input.PoolLayoutVersion
              Round = input.Round
              HomeTeamScore = None
              AwayTeamScore = None
              BetScore = None
              ComputedDateTime = None
              ComputedRequestId = None }

        let rec loop (next: string option) : Async<unit> =
            async {
                let! page = poolRepository.GetGamblersByPoolLayoutAsync(input.PoolLayoutId, next)

                do!
                    page.Items
                    |> Seq.map (fun gambler ->
                        poolGamblerBetRepository.MaterializeMatchForGamblerAsync(
                            buildBet gambler,
                            input.PoolLayoutVersion
                        ))
                    |> Seq.iterAsync id

                match page.Next with
                | Some n -> return! loop (Some n)
                | None -> return ()
            }

        loop None
