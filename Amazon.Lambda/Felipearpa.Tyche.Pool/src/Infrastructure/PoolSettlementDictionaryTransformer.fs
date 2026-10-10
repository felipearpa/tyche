namespace Felipearpa.Tyche.Pool.Infrastructure

open System
open System.Collections.Generic
open System.Globalization
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb.Dictionary
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type

/// Maps pool root checkpoints, receipts, memberships, and match details to the settlement
/// and projection states, and the checkpoint back to its stored map.
module PoolSettlementDictionaryTransformer =

    let private tryString key (dictionary: IDictionary<string, AttributeValue>) =
        dictionary |> tryGetAttributeValueOrNone key |> Option.bind (fun value -> Option.ofObj value.S)

    let private tryInt key (dictionary: IDictionary<string, AttributeValue>) =
        dictionary
        |> tryGetAttributeValueOrNone key
        |> Option.bind (fun value -> Option.ofObj value.N)
        |> Option.map (fun value -> Int32.Parse(value, CultureInfo.InvariantCulture))

    let private tryInt64 key (dictionary: IDictionary<string, AttributeValue>) =
        dictionary
        |> tryGetAttributeValueOrNone key
        |> Option.bind (fun value -> Option.ofObj value.N)
        |> Option.map (fun value -> Int64.Parse(value, CultureInfo.InvariantCulture))

    let private tryBool key (dictionary: IDictionary<string, AttributeValue>) =
        dictionary
        |> tryGetAttributeValueOrNone key
        |> Option.bind (fun value -> if value.BOOL.HasValue then Some value.BOOL.Value else None)

    let private parseInstant (value: string) =
        DateTime.Parse(value, CultureInfo.InvariantCulture, DateTimeStyles.RoundtripKind)

    let private formatInstant (value: DateTime) =
        value.ToUniversalTime().ToString("o", CultureInfo.InvariantCulture)

    let private s (value: string) = AttributeValue(S = value)
    let private n (value: int64) = AttributeValue(N = value.ToString(CultureInfo.InvariantCulture))

    let private prediction (dictionary: IDictionary<string, AttributeValue>) =
        match tryInt PoolTable.Attribute.homeTeamBet dictionary, tryInt PoolTable.Attribute.awayTeamBet dictionary with
        | Some home, Some away ->
            Some
                { TeamScore.HomeTeamValue = BetScore.newOf home
                  AwayTeamValue = BetScore.newOf away }
        | _ -> None

    let private matchIdOf (dictionary: IDictionary<string, AttributeValue>) =
        dictionary[PoolTable.Attribute.matchId].S |> Ulid.newOf

    let toMembershipState (dictionary: IDictionary<string, AttributeValue>) : MembershipState =
        { GamblerId = dictionary[PoolTable.Attribute.gamblerId].S |> Ulid.newOf
          GamblerUsername = dictionary[PoolTable.Attribute.gamblerUsername].S
          GamblerEmail = dictionary[PoolTable.Attribute.gamblerEmail].S
          PoolLayoutId = dictionary[PoolTable.Attribute.poolLayoutId].S |> Ulid.newOf
          Generation = tryString PoolTable.Attribute.membershipGeneration dictionary
          ScoreVersion = tryInt64 PoolTable.Attribute.scoreVersion dictionary
          Total = tryInt PoolTable.Attribute.score dictionary
          BeforeScore = tryInt PoolTable.Attribute.beforeScore dictionary
          Position = tryInt PoolTable.Attribute.position dictionary |> Option.filter (fun value -> value <> 0)
          BeforePosition =
            tryInt PoolTable.Attribute.beforePosition dictionary
            |> Option.filter (fun value -> value <> 0)
          PositionOperationRevision = tryInt64 PoolTable.Attribute.positionOperationRevision dictionary
          PositionCalculationRevision = tryInt PoolTable.Attribute.positionCalculationRevision dictionary }

    let toBetSettlementState (dictionary: IDictionary<string, AttributeValue>) : BetSettlementState =
        { GamblerId = dictionary[PoolTable.Attribute.gamblerId].S |> Ulid.newOf
          MatchId = matchIdOf dictionary
          Generation = tryString PoolTable.Attribute.poolMembershipGeneration dictionary
          Prediction = prediction dictionary
          Award = tryInt PoolTable.Attribute.score dictionary
          ResultVersion = tryInt PoolTable.Attribute.resultVersion dictionary
          IsComputed = dictionary.ContainsKey PoolTable.Attribute.computedRequestId
          PositionOperationRevision = tryInt64 PoolTable.Attribute.positionOperationRevision dictionary }

    let private totalProjection (dictionary: IDictionary<string, AttributeValue>) =
        match tryString PoolTable.Attribute.totalPoolPointsState dictionary with
        | Some "KNOWN" -> tryInt PoolTable.Attribute.totalPoolPoints dictionary |> Option.map KnownTotal
        | Some "UNAVAILABLE" -> Some UnavailableTotal
        | _ -> None

    let toBetProjectionState (dictionary: IDictionary<string, AttributeValue>) : BetProjectionState =
        { GamblerId = dictionary[PoolTable.Attribute.gamblerId].S |> Ulid.newOf
          MatchId = matchIdOf dictionary
          Generation = tryString PoolTable.Attribute.poolMembershipGeneration dictionary
          ScoreVersion = tryInt64 PoolTable.Attribute.poolScoreVersion dictionary
          Active = tryBool PoolTable.Attribute.rankingActive dictionary |> Option.defaultValue false
          Total = totalProjection dictionary
          Award = tryInt PoolTable.Attribute.score dictionary
          IsComputed = dictionary.ContainsKey PoolTable.Attribute.computedRequestId
          HasRankingKey = dictionary.ContainsKey PoolTable.Attribute.getPoolMatchBetsByTotalSk
          RestoreExcludedGeneration = tryString PoolTable.Attribute.restoreExcludedGeneration dictionary }

    let toRetainedBetState (dictionary: IDictionary<string, AttributeValue>) : RetainedBetState =
        { Settlement = toBetSettlementState dictionary
          RestoreExcludedGeneration = tryString PoolTable.Attribute.restoreExcludedGeneration dictionary
          SettlementOperationId = tryString PoolTable.Attribute.settlementOperationId dictionary }

    [<RequireQualifiedAccess>]
    module private Checkpoint =
        let operationId = "operationId"
        let revision = "revision"
        let target = "target"
        let matchId = "matchId"
        let homeTeamScore = "homeTeamScore"
        let awayTeamScore = "awayTeamScore"
        let resultVersion = "resultVersion"
        let gamblerId = "gamblerId"
        let generation = "generation"
        let stage = "stage"
        let cursor = "cursor"
        let calculationRevision = "calculationRevision"
        let membershipRevision = "membershipRevision"
        let ownerToken = "ownerToken"
        let leaseExpiresAt = "leaseExpiresAt"
        let attemptsWithoutProgress = "attemptsWithoutProgress"
        let blocked = "blocked"
        let unresolvedDetails = "unresolvedDetails"
        let restoredDetails = "restoredDetails"
        let admittedAt = "admittedAt"
        let lastProgressAt = "lastProgressAt"

    /// The stored map for the active checkpoint.
    let activeOperationAttribute (active: ActiveOperation) =
        let target =
            match active.Target with
            | MatchSettlement(matchId, result) ->
                [ Checkpoint.target, s "MATCH"
                  Checkpoint.matchId, s matchId.Value
                  Checkpoint.homeTeamScore, n (int64 result.HomeTeamScore)
                  Checkpoint.awayTeamScore, n (int64 result.AwayTeamScore)
                  Checkpoint.resultVersion, n (int64 result.Version) ]
            | CurrentStandings -> [ Checkpoint.target, s "STANDINGS" ]
            | RejoinRestoration(gamblerId, generation) ->
                [ Checkpoint.target, s "RESTORE"
                  Checkpoint.gamblerId, s gamblerId.Value
                  Checkpoint.generation, s generation ]

        let cursor =
            active.Cursor |> Option.map (fun value -> Checkpoint.cursor, s value) |> Option.toList

        let entries =
            [ Checkpoint.operationId, s active.OperationId
              Checkpoint.revision, n active.Revision
              Checkpoint.stage, s (match active.Stage with | Scoring -> "SCORING" | Positions -> "POSITIONS")
              Checkpoint.calculationRevision, n (int64 active.CalculationRevision)
              Checkpoint.membershipRevision, n active.MembershipRevision
              Checkpoint.ownerToken, s active.OwnerToken
              Checkpoint.leaseExpiresAt, s (formatInstant active.LeaseExpiresAt)
              Checkpoint.attemptsWithoutProgress, n (int64 active.AttemptsWithoutProgress)
              Checkpoint.blocked, AttributeValue(BOOL = active.Blocked)
              Checkpoint.unresolvedDetails, n (int64 active.UnresolvedDetails)
              Checkpoint.restoredDetails, n (int64 active.RestoredDetails)
              Checkpoint.admittedAt, s (formatInstant active.AdmittedAt)
              Checkpoint.lastProgressAt, s (formatInstant active.LastProgressAt) ]
            @ target
            @ cursor

        AttributeValue(M = Dictionary(dict entries))

    let toActiveOperation (map: IDictionary<string, AttributeValue>) : ActiveOperation =
        let required key =
            tryString key map |> Option.defaultWith (fun () -> failwith $"Checkpoint is missing {key}")

        let requiredInt64 key =
            tryInt64 key map |> Option.defaultWith (fun () -> failwith $"Checkpoint is missing {key}")

        let target =
            match required Checkpoint.target with
            | "MATCH" ->
                MatchSettlement(
                    required Checkpoint.matchId |> Ulid.newOf,
                    { HomeTeamScore = requiredInt64 Checkpoint.homeTeamScore |> int
                      AwayTeamScore = requiredInt64 Checkpoint.awayTeamScore |> int
                      Version = requiredInt64 Checkpoint.resultVersion |> int }
                )
            | "STANDINGS" -> CurrentStandings
            | "RESTORE" -> RejoinRestoration(required Checkpoint.gamblerId |> Ulid.newOf, required Checkpoint.generation)
            | other -> failwith $"Unknown checkpoint target {other}"

        { OperationId = required Checkpoint.operationId
          Revision = requiredInt64 Checkpoint.revision
          Target = target
          Stage =
            match required Checkpoint.stage with
            | "SCORING" -> Scoring
            | "POSITIONS" -> Positions
            | other -> failwith $"Unknown checkpoint stage {other}"
          Cursor = tryString Checkpoint.cursor map
          CalculationRevision = requiredInt64 Checkpoint.calculationRevision |> int
          MembershipRevision = requiredInt64 Checkpoint.membershipRevision
          OwnerToken = required Checkpoint.ownerToken
          LeaseExpiresAt = required Checkpoint.leaseExpiresAt |> parseInstant
          AttemptsWithoutProgress = requiredInt64 Checkpoint.attemptsWithoutProgress |> int
          Blocked = tryBool Checkpoint.blocked map |> Option.defaultValue false
          UnresolvedDetails = tryInt Checkpoint.unresolvedDetails map |> Option.defaultValue 0
          RestoredDetails = tryInt Checkpoint.restoredDetails map |> Option.defaultValue 0
          AdmittedAt = required Checkpoint.admittedAt |> parseInstant
          LastProgressAt = required Checkpoint.lastProgressAt |> parseInstant }

    let toSettlementRoot (dictionary: IDictionary<string, AttributeValue>) : PoolSettlementRoot =
        { PoolId = dictionary[PoolTable.Attribute.poolId].S |> Ulid.newOf
          PoolLayoutId = dictionary[PoolTable.Attribute.poolLayoutId].S |> Ulid.newOf
          MembershipRevision = tryInt64 PoolTable.Attribute.membershipRevision dictionary |> Option.defaultValue 0L
          OperationRevision = tryInt64 PoolTable.Attribute.operationRevision dictionary |> Option.defaultValue 0L
          LastCompletedOperationRevision =
            tryInt64 PoolTable.Attribute.lastCompletedOperationRevision dictionary
            |> Option.defaultValue 0L
          ActiveOperation =
            dictionary
            |> tryGetAttributeValueOrNone PoolTable.Attribute.activeOperation
            |> Option.bind (fun value -> Option.ofObj value.M)
            |> Option.map toActiveOperation
          RankingReadiness =
            dictionary
            |> tryGetAttributeValueOrNone PoolTable.Attribute.rankingReadiness
            |> Option.bind (fun value -> Option.ofObj value.M)
            |> Option.bind (fun readiness -> tryString "state" readiness)
            |> Option.map (fun state -> if state = "READY" then Ready else NotReady) }

    let private receiptStateName (state: ReceiptState) =
        match state with
        | Admitted -> "ADMITTED"
        | Completed -> "COMPLETED"
        | Deferred -> "DEFERRED"

    let receiptStateValue (state: ReceiptState) = receiptStateName state

    let toReceipt (dictionary: IDictionary<string, AttributeValue>) : OperationReceipt =
        { OperationId = dictionary[PoolTable.Attribute.operationId].S
          Revision = tryInt64 PoolTable.Attribute.operationRevision dictionary
          Fingerprint = dictionary[PoolTable.Attribute.operationFingerprint].S
          State =
            match dictionary[PoolTable.Attribute.operationState].S with
            | "ADMITTED" -> Admitted
            | "COMPLETED" -> Completed
            | "DEFERRED" -> Deferred
            | other -> failwith $"Unknown receipt state {other}"
          Command = dictionary[PoolTable.Attribute.operationCommand].S |> PoolMessageJson.decodeCommandJson
          DeferredAt = tryString PoolTable.Attribute.deferredDateTime dictionary |> Option.map parseInstant }

    /// The receipt item. It carries no score, username, or date attributes, so it never
    /// enters the leaderboard, member, pending, or finished indexes.
    let receiptItem (poolId: Ulid) (receipt: OperationReceipt) (now: DateTime) =
        let poolKey = $"{PoolTable.Prefix.pool}#{poolId.Value}"

        let entries =
            [ "pk", s poolKey
              "sk", s $"{PoolTable.Prefix.settlement}#{receipt.OperationId}"
              PoolTable.Attribute.poolId, s poolId.Value
              PoolTable.Attribute.operationId, s receipt.OperationId
              PoolTable.Attribute.operationFingerprint, s receipt.Fingerprint
              PoolTable.Attribute.operationState, s (receiptStateName receipt.State)
              PoolTable.Attribute.operationCommand, s (PoolMessageJson.encodeCommand receipt.Command) ]
            @ (receipt.Revision |> Option.map (fun revision -> PoolTable.Attribute.operationRevision, n revision) |> Option.toList)
            @ (match receipt.State with
               | Admitted -> [ PoolTable.Attribute.admittedDateTime, s (formatInstant now) ]
               | Completed -> [ PoolTable.Attribute.completedDateTime, s (formatInstant now) ]
               | Deferred -> [ PoolTable.Attribute.deferredDateTime, s (formatInstant now) ])

        Dictionary(dict entries)

    let instant (value: DateTime) = formatInstant value
