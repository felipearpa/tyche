namespace Felipearpa.Tyche.Function.Request

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module JoinPoolRequestTransformer =
    let toJoinPoolInput (poolId: string) (joinPoolRequest: JoinPoolRequest) =
        { JoinPoolInput.PoolId = poolId |> Ulid.newOf
          GamblerId = joinPoolRequest.GamblerId |> Ulid.newOf }
