namespace Felipearpa.Tyche.Function.Response

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module CreatePoolOutputTransformer =
    let toResponse (createPoolOutput: CreatePoolOutput) : PoolResponse =
        { PoolResponse.PoolId = createPoolOutput.PoolId |> Ulid.value
          PoolName = createPoolOutput.PoolName |> NonEmptyString100.value }
