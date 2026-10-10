namespace Felipearpa.Tyche.Pool.Domain

open System
open Felipearpa.Core.Paging
open Felipearpa.Type

/// Durably enqueues pool commands on the ordered per-pool queue. Completes only when every
/// command was accepted; otherwise raises.
type IPoolCommandPublisher =
    abstract PublishAsync: PoolCommand list -> Async<unit>

    /// Enqueues commands again on purpose, such as deferred commands after a blocked pool
    /// recovers, so the queue's deduplication window cannot drop them.
    abstract RepublishAsync: PoolCommand list -> Async<unit>

/// Durably enqueues maintenance jobs. Completes only when every job was accepted.
type IPoolMaintenancePublisher =
    abstract PublishAsync: PoolMaintenanceJob list -> Async<unit>

/// A layout match as the Pool context needs it, read strongly from the PoolLayout context.
type PoolLayoutMatchSnapshot =
    { MatchId: Ulid
      PoolLayoutId: Ulid
      PoolLayoutVersion: int
      HomeTeamId: string
      HomeTeamName: NonEmptyString100
      AwayTeamId: string
      AwayTeamName: NonEmptyString100
      MatchDateTime: DateTime
      Round: string
      GroupName: string option
      Result: MatchResult option }

type IPoolLayoutMatchReader =
    abstract GetMatchAsync: poolLayoutId: Ulid * matchId: Ulid -> Async<PoolLayoutMatchSnapshot option>

    /// Strongly consistent page of the layout's matches up to and including the version.
    abstract GetMatchesThroughVersionAsync:
        poolLayoutId: Ulid * poolLayoutVersion: int * next: string option -> Async<PoolLayoutMatchSnapshot CursorPage>
