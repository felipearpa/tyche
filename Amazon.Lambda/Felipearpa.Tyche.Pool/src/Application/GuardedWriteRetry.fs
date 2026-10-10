namespace Felipearpa.Tyche.Pool.Application

open System

/// Backoff for guarded writes that met transaction contention and are tried again.
[<RequireQualifiedAccess>]
module GuardedWriteRetry =

    /// A jittered delay that doubles with every attempt, from 10–20 ms up to the sixth
    /// doubling.
    let backoff (attempt: int) =
        let delay = 20 * (1 <<< min attempt 6)
        Async.Sleep(Random.Shared.Next(delay / 2, delay + 1))
