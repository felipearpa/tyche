namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Type

type JoinPoolDomainFailure = | AlreadyJoined

type IPoolRepository =
    abstract GetPoolById: Ulid -> Result<Option<Pool>, unit> Async
    abstract CreatePool: ResolvedCreatePoolInput -> Result<CreatePoolOutput, unit> Async
    abstract JoinPool: ResolvedJoinPoolInput -> Result<unit, JoinPoolDomainFailure> Async
