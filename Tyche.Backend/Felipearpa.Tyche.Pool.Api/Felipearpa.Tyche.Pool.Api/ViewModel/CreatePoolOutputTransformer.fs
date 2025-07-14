namespace Felipearpa.Tyche.Pool.Api.ViewModel

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module CreatePoolOutputTransformer =
    let toPoolViewModel (createPoolOutput: CreatePoolOutput) =
        { PoolViewModel.PoolId = createPoolOutput.PoolId |> Ulid.value
          PoolName = createPoolOutput.PoolName |> NonEmptyString100.value }
