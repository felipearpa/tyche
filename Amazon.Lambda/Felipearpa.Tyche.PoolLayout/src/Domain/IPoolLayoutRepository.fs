namespace Felipearpa.Tyche.PoolLayout.Domain

open Felipearpa.Core.Paging
open Felipearpa.Type
open Microsoft.FSharp.Core

type IPoolLayoutRepository =

    abstract GetOpenPoolLayoutsAsync: string option -> Async<PoolLayout CursorPage>

    abstract GetPoolLayoutByIdAsync: Ulid -> Async<PoolLayout option>

    abstract GetPoolLayoutMatchesThroughVersionAsync: Ulid * int * string option -> Async<PoolLayoutMatch CursorPage>

    abstract SetMatchFinalScoreAsync: Ulid * Ulid * int * int -> Async<unit>

    /// Strongly consistent read of one layout match.
    abstract GetMatchAsync: poolLayoutId: Ulid * matchId: Ulid -> Async<PoolLayoutMatch option>

    /// Replaces the result with the next version when the current version is the expected
    /// one. False when the match changed or has no result.
    abstract CorrectMatchResultAsync:
        poolLayoutId: Ulid * matchId: Ulid * homeTeamScore: int * awayTeamScore: int * expectedVersion: int ->
            Async<bool>
