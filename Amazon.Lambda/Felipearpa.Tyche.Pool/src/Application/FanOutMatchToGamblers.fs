namespace Felipearpa.Tyche.Pool.Application

open System
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
      Round: string
      GroupName: string option }

/// Materializes a match added to a layout into every member's bet sheet. Details are created
/// uncomputed through the guarded initializer. Afterwards the match is read again: if it has
/// a result by then, a match reconciliation named after the source event is durably requested
/// for every pool that received details, because no later result event may arrive, the match
/// index may not show them yet, and the result's own operation may already have completed.
type FanOutMatchToGamblers
    (
        poolRepository: IPoolRepository,
        betRepository: IPoolGamblerBetRepository,
        projectionRepository: IPoolProjectionRepository,
        layoutMatchReader: IPoolLayoutMatchReader,
        commandPublisher: IPoolCommandPublisher
    ) =

    [<Literal>]
    let concurrency = 8

    let snapshotOf (input: FanOutMatchInput) : PoolLayoutMatchSnapshot =
        { MatchId = input.MatchId
          PoolLayoutId = input.PoolLayoutId
          PoolLayoutVersion = input.PoolLayoutVersion
          HomeTeamId = input.HomeTeamId
          HomeTeamName = input.HomeTeamName
          AwayTeamId = input.AwayTeamId
          AwayTeamName = input.AwayTeamName
          MatchDateTime = input.MatchDateTime
          Round = input.Round
          GroupName = input.GroupName
          Result = None }

    let materialize (layoutMatch: PoolLayoutMatchSnapshot) (gambler: PoolLayoutGambler) =
        async {
            let bet = BetMaterialization.uncomputedBet gambler layoutMatch

            match! BetMaterialization.materialize betRepository projectionRepository bet 0 with
            | MembershipGone -> return None
            | Created
            | Existing ->
                do! betRepository.RaisePoolLayoutVersionAsync(gambler.PoolId, gambler.GamblerId, layoutMatch.PoolLayoutVersion)
                return Some gambler.PoolId
        }

    /// The source identifies the stream event that inserted the match, so a redelivery reuses
    /// the same reconciliation identity.
    member this.ExecuteAsync(input: FanOutMatchInput, source: string) : Async<unit> =
        let layoutMatch = snapshotOf input

        let rec loop (next: string option) (pools: Set<Ulid>) =
            async {
                let! page = poolRepository.GetGamblersByPoolLayoutAsync(input.PoolLayoutId, next)

                let! touched =
                    Async.Parallel(page.Items |> Seq.map (materialize layoutMatch) |> List.ofSeq, concurrency)

                let pools = touched |> Array.choose id |> Array.fold (fun set poolId -> Set.add poolId set) pools

                match page.Next with
                | Some _ -> return! loop page.Next pools
                | None -> return pools
            }

        async {
            let! pools = loop None Set.empty
            let! current = layoutMatchReader.GetMatchAsync(input.PoolLayoutId, input.MatchId)

            match current |> Option.bind _.Result with
            | Some _ when not pools.IsEmpty ->
                do!
                    commandPublisher.PublishAsync(
                        pools
                        |> Set.toList
                        |> List.map (fun poolId -> PoolCommand.reconcileMatch poolId input.MatchId source)
                    )
            | _ -> ()
        }
