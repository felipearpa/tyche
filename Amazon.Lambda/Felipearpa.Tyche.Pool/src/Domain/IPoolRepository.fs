namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Core.Paging
open Felipearpa.Type

type JoinPoolDomainFailure = | AlreadyJoined

type IPoolRepository =
    abstract GetPoolByIdAsync: Ulid -> Result<Option<Pool>, unit> Async
    abstract CreatePoolAsync: ResolvedCreatePoolInput -> Result<CreatePoolOutput, unit> Async
    abstract JoinPoolAsync: ResolvedJoinPoolInput -> Result<unit, JoinPoolDomainFailure> Async
    abstract IsPoolMemberAsync: Ulid * Ulid -> Result<bool, unit> Async
    abstract GetGamblersByPoolLayoutAsync: Ulid * string option -> Async<PoolLayoutGambler CursorPage>
    abstract DeletePoolAsync: Ulid -> unit Async
    abstract PropagateGamblerUsernameAsync: Ulid * NonEmptyString100 -> unit Async
