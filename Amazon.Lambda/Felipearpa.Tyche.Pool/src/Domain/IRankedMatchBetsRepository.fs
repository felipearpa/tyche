namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Core.Paging
open Felipearpa.Type

type RankedCursorError =
    /// Not a cursor this service issued, or issued by an incompatible key schema.
    | MalformedCursor
    /// Issued for another pool, match, or ordering mode.
    | CursorContextMismatch

/// Reads complete participant rows of one pool and match from the dedicated ranking index,
/// ordered by the propagated pool total. The index is eventually consistent.
type IRankedMatchBetsRepository =
    abstract GetPageAsync:
        poolId: Ulid * matchId: Ulid * cursor: string option * limit: int ->
            Async<Result<PoolGamblerBet CursorPage, RankedCursorError>>
