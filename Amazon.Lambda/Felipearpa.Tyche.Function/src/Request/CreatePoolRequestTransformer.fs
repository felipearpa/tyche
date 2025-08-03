namespace Felipearpa.Tyche.Function.Request

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module CreatePoolRequestTransformer =
    let toCreatePoolInput (createPoolRequest: CreatePoolRequest) =
        { CreatePoolInput.PoolId = Ulid.random ()
          PoolName = createPoolRequest.PoolName |> NonEmptyString100.newOf
          PoolLayoutId = createPoolRequest.PoolLayoutId |> Ulid.newOf
          OwnerGamblerId = createPoolRequest.OwnerGamblerId |> Ulid.newOf }
