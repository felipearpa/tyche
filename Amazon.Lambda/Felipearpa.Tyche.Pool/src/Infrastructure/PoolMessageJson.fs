namespace Felipearpa.Tyche.Pool.Infrastructure

open System.IO
open System.Text
open System.Text.Json
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

/// Versioned JSON bodies for pool commands and maintenance jobs.
///
/// Command: {"schema":1,"operationId":"RESULT#…#1","poolId":"…","kind":"SETTLE_RESULT",
/// "matchId":"…","homeTeamScore":1,"awayTeamScore":0,"resultVersion":1}
///
/// Positions messages written before this schema have the body "poolId|matchId"; they
/// decode as LegacyPositions and are translated by the consumer.
module PoolMessageJson =

    [<Literal>]
    let schema = 1

    type DecodedPoolMessage =
        | Command of PoolCommand
        | LegacyPositions of poolId: Ulid * matchId: Ulid

    let private write (build: Utf8JsonWriter -> unit) =
        use stream = new MemoryStream()

        do
            use writer = new Utf8JsonWriter(stream)
            writer.WriteStartObject()
            writer.WriteNumber("schema", schema)
            build writer
            writer.WriteEndObject()

        Encoding.UTF8.GetString(stream.ToArray())

    let encodeCommand (command: PoolCommand) =
        write (fun writer ->
            writer.WriteString("operationId", command.OperationId)
            writer.WriteString("poolId", command.PoolId.Value)

            match command.Kind with
            | SettleResult(matchId, result) ->
                writer.WriteString("kind", "SETTLE_RESULT")
                writer.WriteString("matchId", matchId.Value)
                writer.WriteNumber("homeTeamScore", result.HomeTeamScore)
                writer.WriteNumber("awayTeamScore", result.AwayTeamScore)
                writer.WriteNumber("resultVersion", result.Version)
            | ReconcileMatch matchId ->
                writer.WriteString("kind", "RECONCILE_MATCH")
                writer.WriteString("matchId", matchId.Value)
            | ReconcileStandings -> writer.WriteString("kind", "RECONCILE_STANDINGS")
            | RestoreRejoinHistory(gamblerId, generation) ->
                writer.WriteString("kind", "RESTORE_REJOIN_HISTORY")
                writer.WriteString("gamblerId", gamblerId.Value)
                writer.WriteString("membershipGeneration", generation))

    let private requireSchema (root: JsonElement) =
        match root.TryGetProperty "schema" with
        | true, value when value.GetInt32() = schema -> ()
        | _ -> raise (InvalidDataException "Unsupported pool message schema")

    let private text (root: JsonElement) (name: string) = root.GetProperty(name).GetString()

    let private optionalText (root: JsonElement) (name: string) =
        match root.TryGetProperty name with
        | true, value when value.ValueKind = JsonValueKind.String -> Some(value.GetString())
        | _ -> None

    let private number (root: JsonElement) (name: string) = root.GetProperty(name).GetInt32()

    let decodeCommandJson (body: string) =
        use document = JsonDocument.Parse body
        let root = document.RootElement
        requireSchema root
        let poolId = text root "poolId" |> Ulid.newOf

        let kind =
            match text root "kind" with
            | "SETTLE_RESULT" ->
                SettleResult(
                    text root "matchId" |> Ulid.newOf,
                    { HomeTeamScore = number root "homeTeamScore"
                      AwayTeamScore = number root "awayTeamScore"
                      Version = number root "resultVersion" }
                )
            | "RECONCILE_MATCH" -> ReconcileMatch(text root "matchId" |> Ulid.newOf)
            | "RECONCILE_STANDINGS" -> ReconcileStandings
            | "RESTORE_REJOIN_HISTORY" ->
                RestoreRejoinHistory(text root "gamblerId" |> Ulid.newOf, text root "membershipGeneration")
            | other -> raise (InvalidDataException $"Unknown pool command kind {other}")

        { OperationId = text root "operationId"
          PoolId = poolId
          Kind = kind }

    let decodePoolMessage (body: string) =
        if body.TrimStart().StartsWith "{" then
            Command(decodeCommandJson body)
        else
            match body.Split('|') with
            | [| poolId; matchId |] -> LegacyPositions(Ulid.newOf poolId, Ulid.newOf matchId)
            | _ -> raise (InvalidDataException "Unrecognized pool message body")

    let encodeJob (job: PoolMaintenanceJob) =
        let cursor (writer: Utf8JsonWriter) (value: string option) =
            value |> Option.iter (fun value -> writer.WriteString("cursor", value))

        write (fun writer ->
            match job with
            | PropagateTotal(poolId, gamblerId, afterMatchId) ->
                writer.WriteString("kind", "PROPAGATE_TOTAL")
                writer.WriteString("poolId", poolId.Value)
                writer.WriteString("gamblerId", gamblerId.Value)
                cursor writer afterMatchId
            | InitializeIdentities(poolId, afterGamblerId) ->
                writer.WriteString("kind", "INITIALIZE_IDENTITIES")
                writer.WriteString("poolId", poolId.Value)
                cursor writer afterGamblerId
            | BackfillProjection(poolId, afterGamblerId) ->
                writer.WriteString("kind", "BACKFILL_PROJECTION")
                writer.WriteString("poolId", poolId.Value)
                cursor writer afterGamblerId
            | VerifyReadiness poolId ->
                writer.WriteString("kind", "VERIFY_READINESS")
                writer.WriteString("poolId", poolId.Value)
            | ReconcilePool(poolId, afterGamblerId) ->
                writer.WriteString("kind", "RECONCILE_POOL")
                writer.WriteString("poolId", poolId.Value)
                cursor writer afterGamblerId
            | RetireMembership(poolId, gamblerId, generation) ->
                writer.WriteString("kind", "RETIRE_MEMBERSHIP")
                writer.WriteString("poolId", poolId.Value)
                writer.WriteString("gamblerId", gamblerId.Value)
                generation |> Option.iter (fun value -> writer.WriteString("generation", value))
            | BackfillKickoffGroups(poolId, afterMatchId) ->
                writer.WriteString("kind", "BACKFILL_KICKOFF_GROUPS")
                writer.WriteString("poolId", poolId.Value)
                cursor writer afterMatchId)

    let decodeJob (body: string) =
        use document = JsonDocument.Parse body
        let root = document.RootElement
        requireSchema root
        let poolId = text root "poolId" |> Ulid.newOf
        let cursor = optionalText root "cursor"

        match text root "kind" with
        | "PROPAGATE_TOTAL" -> PropagateTotal(poolId, text root "gamblerId" |> Ulid.newOf, cursor)
        | "INITIALIZE_IDENTITIES" -> InitializeIdentities(poolId, cursor)
        | "BACKFILL_PROJECTION" -> BackfillProjection(poolId, cursor)
        | "VERIFY_READINESS" -> VerifyReadiness poolId
        | "RECONCILE_POOL" -> ReconcilePool(poolId, cursor)
        | "RETIRE_MEMBERSHIP" ->
            RetireMembership(poolId, text root "gamblerId" |> Ulid.newOf, optionalText root "generation")
        | "BACKFILL_KICKOFF_GROUPS" -> BackfillKickoffGroups(poolId, cursor)
        | other -> raise (InvalidDataException $"Unknown maintenance job kind {other}")

    /// The FIFO deduplication ID for a command: its pool and operation ID, hashed when longer
    /// than the 128 characters SQS allows. Operation IDs repeat across pools (every pool's
    /// result command for a match has the same one) and deduplication is queue-wide, so the
    /// pool must be part of the ID.
    let deduplicationId (command: PoolCommand) =
        let scoped = $"{command.PoolId.Value}#{command.OperationId}"

        if scoped.Length <= 128 then
            scoped
        else
            let hash = System.Security.Cryptography.SHA256.HashData(Encoding.UTF8.GetBytes scoped)
            System.Convert.ToHexString hash
