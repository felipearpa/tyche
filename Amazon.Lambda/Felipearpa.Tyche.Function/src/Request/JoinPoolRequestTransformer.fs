namespace Felipearpa.Tyche.Function.Request

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module JoinPoolRequestTransformer =
    let toJoinPoolInput (joinPoolRequest: JoinPoolRequest) =
        { JoinPoolInput.PoolId = joinPoolRequest.PoolId |> Ulid.newOf
          GamblerId = joinPoolRequest.GamblerId |> Ulid.newOf }
