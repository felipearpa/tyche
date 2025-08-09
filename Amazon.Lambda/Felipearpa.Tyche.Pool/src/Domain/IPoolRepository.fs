namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Type

type JoinPoolDomainFailure = | AlreadyJoined

type IPoolRepository =
    abstract GetPoolByIdAsync: Ulid -> Result<Option<Pool>, unit> Async
    abstract CreatePoolAsync: ResolvedCreatePoolInput -> Result<CreatePoolOutput, unit> Async
    abstract JoinPoolAsync: ResolvedJoinPoolInput -> Result<unit, JoinPoolDomainFailure> Async
