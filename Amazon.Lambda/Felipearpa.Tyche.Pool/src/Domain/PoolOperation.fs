namespace Felipearpa.Tyche.Pool.Domain

open System
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type

/// An authoritative match result. Version 1 is the first published result; every supported
/// correction publishes the next version.
type MatchResult =
    { HomeTeamScore: int
      AwayTeamScore: int
      Version: int }

    member this.Score =
        { TeamScore.HomeTeamValue = this.HomeTeamScore
          AwayTeamValue = this.AwayTeamScore }

/// The work a pool command requests.
type PoolCommandKind =
    /// Settle a specific result version for the match, then finalize positions.
    | SettleResult of matchId: Ulid * result: MatchResult
    /// Settle whatever result the match has when the command is admitted, then finalize
    /// positions. Requested when details appear after a result was handled.
    | ReconcileMatch of matchId: Ulid
    /// Recalculate current membership positions without a target match.
    | ReconcileStandings

/// A durable, versioned request for one ordered pool operation. Every delivery attempt of the
/// same logical request carries the same OperationId.
type PoolCommand =
    { OperationId: string
      PoolId: Ulid
      Kind: PoolCommandKind }

[<RequireQualifiedAccess>]
module PoolCommand =

    let settleResult (poolId: Ulid) (matchId: Ulid) (result: MatchResult) =
        { OperationId = $"RESULT#{matchId.Value}#{result.Version}"
          PoolId = poolId
          Kind = SettleResult(matchId, result) }

    /// A match reconciliation identified by the source that requested it, so redelivery of
    /// that source reuses the identity while a later source gets a new one.
    let reconcileMatch (poolId: Ulid) (matchId: Ulid) (source: string) =
        { OperationId = $"MATCH#{matchId.Value}#{source}"
          PoolId = poolId
          Kind = ReconcileMatch matchId }

    let reconcileStandings (poolId: Ulid) (source: string) =
        { OperationId = $"STANDINGS#{source}"
          PoolId = poolId
          Kind = ReconcileStandings }

    /// Identifies the immutable payload; reusing an operation ID with another payload is an error.
    let fingerprint (command: PoolCommand) =
        match command.Kind with
        | SettleResult(matchId, result) ->
            $"SETTLE_RESULT|{command.PoolId.Value}|{matchId.Value}|{result.Version}|{result.HomeTeamScore}|{result.AwayTeamScore}"
        | ReconcileMatch matchId -> $"RECONCILE_MATCH|{command.PoolId.Value}|{matchId.Value}"
        | ReconcileStandings -> $"RECONCILE_STANDINGS|{command.PoolId.Value}"

/// Background work that may run in any order because its writes are version guarded.
type PoolMaintenanceJob =
    /// Copy the member's current total to their match details, from the cursor on.
    | PropagateTotal of poolId: Ulid * gamblerId: Ulid * afterMatchId: string option
    /// Initialize missing membership and detail identities, from the membership cursor on.
    | InitializeIdentities of poolId: Ulid * afterGamblerId: string option
    /// Initialize missing projections on existing details, from the membership cursor on.
    | BackfillProjection of poolId: Ulid * afterGamblerId: string option
    /// Check projection coverage and parity and record the pool's readiness.
    | VerifyReadiness of poolId: Ulid
    /// Find and request missing settlement, positions, and projection work.
    | ReconcilePool of poolId: Ulid * afterGamblerId: string option
    /// Retire a removed membership's generation from its details, as the removal stream
    /// handler does; used to replay a retained removal record.
    | RetireMembership of poolId: Ulid * gamblerId: Ulid * generation: string option

/// What an admitted operation does. Match reconciliation resolves to a concrete result at
/// admission, so every resume applies the same immutable input.
type OperationTarget =
    | MatchSettlement of matchId: Ulid * result: MatchResult
    | CurrentStandings

type OperationStage =
    | Scoring
    | Positions

/// The fixed-size checkpoint of the single active operation, stored on the pool root.
type ActiveOperation =
    { OperationId: string
      Revision: int64
      Target: OperationTarget
      Stage: OperationStage
      /// Last membership gambler ID whose settlement finished in the current pass.
      Cursor: string option
      CalculationRevision: int
      /// Membership revision captured when the current pass started.
      MembershipRevision: int64
      OwnerToken: string
      LeaseExpiresAt: DateTime
      AttemptsWithoutProgress: int
      Blocked: bool
      /// Details of the current settlement pass that could not be settled yet: missing
      /// (pending materialization), unbound, or malformed.
      UnresolvedDetails: int
      AdmittedAt: DateTime
      LastProgressAt: DateTime }

type RankingReadiness =
    | Ready
    | NotReady

type PoolSettlementRoot =
    { PoolId: Ulid
      PoolLayoutId: Ulid
      MembershipRevision: int64
      OperationRevision: int64
      LastCompletedOperationRevision: int64
      ActiveOperation: ActiveOperation option
      RankingReadiness: RankingReadiness option }

type ReceiptState =
    | Admitted
    | Completed
    /// Held outside the active fence while the pool is blocked; re-enqueued in order on recovery.
    | Deferred

/// A small receipt for one logical operation, kept for the pool's lifetime.
type OperationReceipt =
    { OperationId: string
      Revision: int64 option
      Fingerprint: string
      State: ReceiptState
      Command: PoolCommand
      DeferredAt: DateTime option }

/// Authoritative membership state read strongly from the base table.
type MembershipState =
    { GamblerId: Ulid
      GamblerUsername: string
      GamblerEmail: string
      PoolLayoutId: Ulid
      Generation: string option
      ScoreVersion: int64 option
      Total: int option
      BeforeScore: int option
      Position: int option
      BeforePosition: int option
      PositionOperationRevision: int64 option
      PositionCalculationRevision: int option }

/// The settlement-relevant state of one match detail, read strongly from the base table.
type BetSettlementState =
    { GamblerId: Ulid
      MatchId: Ulid
      Generation: string option
      Prediction: TeamScore<BetScore> option
      Award: int option
      ResultVersion: int option
      IsComputed: bool
      PositionOperationRevision: int64 option }

/// The pool-total copy carried by a match detail.
type TotalProjection =
    | KnownTotal of int
    | UnavailableTotal

/// The projection-relevant state of one match detail.
type BetProjectionState =
    { GamblerId: Ulid
      MatchId: Ulid
      Generation: string option
      ScoreVersion: int64 option
      Active: bool
      Total: TotalProjection option
      Award: int option
      IsComputed: bool
      HasRankingKey: bool }

/// The membership a detail is bound to, captured for a guarded write.
type MembershipBinding =
    { Generation: string
      ScoreVersion: int64
      Total: TotalProjection }

/// Outcome of a guarded write. Throttling and transport failures are raised, not returned.
type GuardedWriteOutcome =
    | Written
    /// A condition on one of the written records did not hold.
    | ConditionNotMet
    /// A concurrent transaction touched one of the records.
    | Contended

[<RequireQualifiedAccess>]
module MembershipBinding =

    /// The binding for a membership with initialized identity. A membership without a total
    /// projects as unavailable rather than zero.
    let tryOf (membership: MembershipState) =
        match membership.Generation, membership.ScoreVersion with
        | Some generation, Some scoreVersion ->
            Some
                { Generation = generation
                  ScoreVersion = scoreVersion
                  Total =
                    membership.Total
                    |> Option.map KnownTotal
                    |> Option.defaultValue UnavailableTotal }
        | _ -> None
