namespace Felipearpa.Tyche.Pool.Application

open System

/// Work budgets for one ordered pool operation invocation. See docs/pool-settlement.md for
/// how these relate to the Lambda timeout, queue visibility timeout, and redrive limit.
type PoolOperationSettings =
    {
        /// Memberships read per strongly consistent page; also the settlement checkpoint interval.
        MembershipPageSize: int
        /// Concurrent guarded settlement or position transactions within one pool.
        WriteConcurrency: int
        /// Attempts for one guarded write that keeps meeting contention or changed state.
        GuardedWriteAttempts: int
        /// How long a claimed checkpoint stays owned; at least the worker's timeout.
        LeaseDuration: TimeSpan
        /// Claims without any checkpointed progress before the operation is marked blocked.
        AttemptsBeforeBlocked: int
    }

    static member Default =
        { MembershipPageSize = 100
          WriteConcurrency = 16
          GuardedWriteAttempts = 6
          LeaseDuration = TimeSpan.FromMinutes 5.0
          AttemptsBeforeBlocked = 5 }
