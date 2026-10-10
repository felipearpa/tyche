namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

/// Turns an accepted match result into one durable settlement command per affected pool,
/// before any settlement happens. Discovery walks every page of the match index, including
/// already computed details, so a retry after partial dispatch reaches every pool again.
/// The index is eventually consistent; materialization and reconciliation cover pools it
/// has not shown yet.
type DispatchMatchResult(repository: IPoolSettlementRepository, commandPublisher: IPoolCommandPublisher) =

    member this.ExecuteAsync(matchId: Ulid, result: MatchResult) : Async<int> =
        let rec dispatch (next: string option) (dispatched: Set<Ulid>) =
            async {
                let! poolIds, next = repository.GetPoolsWithMatchPageAsync(matchId, next)
                let fresh = poolIds |> List.filter (fun poolId -> not (dispatched.Contains poolId))

                if not fresh.IsEmpty then
                    do! commandPublisher.PublishAsync(fresh |> List.map (fun poolId -> PoolCommand.settleResult poolId matchId result))

                let dispatched = fresh |> List.fold (fun set poolId -> Set.add poolId set) dispatched

                match next with
                | Some _ -> return! dispatch next dispatched
                | None -> return dispatched.Count
            }

        dispatch None Set.empty
