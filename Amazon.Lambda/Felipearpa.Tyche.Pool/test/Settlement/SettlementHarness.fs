namespace Felipearpa.Tyche.Pool.Test.Settlement

open Felipearpa.Tyche.Pool.Test.Fakes

/// A PoolLayout stream record as the result handler observes it.
type LayoutRecord =
    | MatchInserted of PoolScenario.MatchSeed
    | ResultChanged of matchId: string * home: int * away: int

/// Drives one implementation of the result → settlement → positions flow against the fake
/// table and queue so the same failure-path scenarios can run before and after refactoring.
type ISettlementHarness =
    abstract Fake: FakeDynamoDb
    abstract Queue: FakeFifoQueue

    abstract SeedAsync: PoolScenario.MemberSeed list * PoolScenario.MatchSeed list -> Async<unit>

    /// Writes the layout changes and runs the stream handler for them as one delivery.
    /// Raises when the handler fails, as a stream delivery would be retried.
    abstract HandleLayoutAsync: LayoutRecord list -> Async<unit>

    /// Delivers queued pool messages until the queue drains.
    abstract DeliverAsync: unit -> Async<unit>

    /// Delivers the pool message originally produced for the match again, after the
    /// queue's deduplication window has passed.
    abstract RedeliverAsync: matchId: string -> Async<unit>
