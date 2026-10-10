namespace Felipearpa.Tyche.Pool.Application

open System
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

/// Materializes a new membership's bet sheet from the layout's matches, read strongly.
/// Matches that already have a result follow the existing late-join rule. A match whose
/// result appears while its detail is being created uncomputed gets a durable match
/// reconciliation, identified by the source event so redelivery reuses it.
type MaterializeJoinedGambler
    (
        betRepository: IPoolGamblerBetRepository,
        projectionRepository: IPoolProjectionRepository,
        layoutMatchReader: IPoolLayoutMatchReader,
        commandPublisher: IPoolCommandPublisher,
        now: unit -> DateTime
    ) =

    let materializePage (gambler: PoolLayoutGambler) (computedRequestId: string) (matches: PoolLayoutMatchSnapshot list) =
        let rec loop (remaining: PoolLayoutMatchSnapshot list) (uncomputed: Ulid list) =
            async {
                match remaining with
                | [] -> return Some uncomputed
                | layoutMatch :: rest ->
                    let bet = BetMaterialization.lateJoinBet gambler layoutMatch (now ()) computedRequestId

                    match! BetMaterialization.materialize betRepository projectionRepository bet 0 with
                    | MembershipGone -> return None
                    | Created when bet.ComputedRequestId.IsNone -> return! loop rest (layoutMatch.MatchId :: uncomputed)
                    | Created
                    | Existing -> return! loop rest uncomputed
            }

        loop matches []

    let resultsArrivedSince (gambler: PoolLayoutGambler) (matchIds: Ulid list) =
        async {
            let! states =
                matchIds
                |> List.map (fun matchId -> layoutMatchReader.GetMatchAsync(gambler.PoolLayoutId, matchId))
                |> Async.Sequential

            return
                states
                |> Array.choose id
                |> Array.filter (fun state -> state.Result.IsSome)
                |> Array.map _.MatchId
                |> List.ofArray
        }

    member this.ExecuteAsync(gambler: PoolLayoutGambler, source: string) : Async<unit> =
        let computedRequestId = Ulid.random().Value

        let rec loop (next: string option) (uncomputed: Ulid list) =
            async {
                let! page =
                    layoutMatchReader.GetMatchesThroughVersionAsync(gambler.PoolLayoutId, gambler.PoolLayoutVersion, next)

                match! materializePage gambler computedRequestId (List.ofSeq page.Items) with
                | None -> return None
                | Some created ->
                    match page.Next with
                    | Some _ -> return! loop page.Next (created @ uncomputed)
                    | None -> return Some(created @ uncomputed)
            }

        async {
            match! loop None [] with
            | None -> ()
            | Some uncomputed ->
                let! settled = resultsArrivedSince gambler uncomputed

                if not settled.IsEmpty then
                    do!
                        commandPublisher.PublishAsync(
                            settled |> List.map (fun matchId -> PoolCommand.reconcileMatch gambler.PoolId matchId source)
                        )
        }
