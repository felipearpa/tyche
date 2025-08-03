namespace Felipearpa.Tyche.Function.Response

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module CreatePoolOutputTransformer =
    let toPoolViewModel (createPoolOutput: CreatePoolOutput) =
        { PoolResponse.PoolId = createPoolOutput.PoolId |> Ulid.value
          PoolName = createPoolOutput.PoolName |> NonEmptyString100.value }
