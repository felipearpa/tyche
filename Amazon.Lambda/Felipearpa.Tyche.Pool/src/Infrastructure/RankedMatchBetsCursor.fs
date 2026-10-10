namespace Felipearpa.Tyche.Pool.Infrastructure

open System
open System.Collections.Generic
open System.IO
open System.Text
open System.Text.Json
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

/// Opaque continuation for ranked match bets: the native index start key bound to the pool,
/// match, ordering mode, cursor version, and ranking key schema it was issued for.
module RankedMatchBetsCursor =

    [<Literal>]
    let version = 1

    [<Literal>]
    let mode = "pool-score-desc"

    let private toBase64Url (bytes: byte array) =
        Convert.ToBase64String(bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')

    let private fromBase64Url (text: string) =
        let padded = text.Replace('-', '+').Replace('_', '/') + String('=', (4 - text.Length % 4) % 4)
        Convert.FromBase64String padded

    let encode (poolId: Ulid) (matchId: Ulid) (lastEvaluatedKey: IDictionary<string, AttributeValue>) =
        use stream = new MemoryStream()

        do
            use writer = new Utf8JsonWriter(stream)
            writer.WriteStartObject()
            writer.WriteNumber("v", version)
            writer.WriteString("mode", mode)
            writer.WriteNumber("schema", RankingKey.schemaVersion)
            writer.WriteString("pool", poolId.Value)
            writer.WriteString("match", matchId.Value)
            writer.WriteString("pk", lastEvaluatedKey[Key.pk].S)
            writer.WriteString("sk", lastEvaluatedKey[Key.sk].S)
            writer.WriteString("rsk", lastEvaluatedKey[PoolTable.Attribute.getPoolMatchBetsByTotalSk].S)
            writer.WriteEndObject()

        toBase64Url (stream.ToArray())

    type private Decoded =
        { Version: int
          Mode: string
          Schema: int
          Pool: string
          Match: string
          Pk: string
          Sk: string
          RankingSortKey: string }

    let private tryRead (cursor: string) =
        try
            use document = JsonDocument.Parse(Encoding.UTF8.GetString(fromBase64Url cursor))
            let root = document.RootElement
            let text (name: string) = root.GetProperty(name).GetString()

            Some
                { Version = root.GetProperty("v").GetInt32()
                  Mode = text "mode"
                  Schema = root.GetProperty("schema").GetInt32()
                  Pool = text "pool"
                  Match = text "match"
                  Pk = text "pk"
                  Sk = text "sk"
                  RankingSortKey = text "rsk" }
        with
        | :? FormatException
        | :? JsonException
        | :? KeyNotFoundException
        | :? InvalidOperationException
        | :? ArgumentException -> None

    /// The start key for the cursor, built from the requested pool and match so a cursor can
    /// never select another context's partition.
    let tryDecode (poolId: Ulid) (matchId: Ulid) (cursor: string) : Result<Dictionary<string, AttributeValue>, RankedCursorError> =
        match tryRead cursor with
        | None -> Error MalformedCursor
        | Some decoded when decoded.Version <> version || decoded.Schema <> RankingKey.schemaVersion -> Error MalformedCursor
        | Some decoded when decoded.Mode <> mode || decoded.Pool <> poolId.Value || decoded.Match <> matchId.Value ->
            Error CursorContextMismatch
        | Some decoded when
            not (decoded.Pk.EndsWith($"#{PoolTable.Prefix.pool}#{poolId.Value}", StringComparison.Ordinal))
            || decoded.Sk <> KeyPrefix.build PoolTable.Prefix.match' matchId.Value
            || RankingKey.trySchemaVersion decoded.RankingSortKey <> Some RankingKey.schemaVersion
            ->
            Error MalformedCursor
        | Some decoded ->
            Ok(
                Dictionary(
                    dict
                        [ Key.pk, AttributeValue(S = decoded.Pk)
                          Key.sk, AttributeValue(S = decoded.Sk)
                          PoolTable.Attribute.getPoolMatchBetsByTotalPk, AttributeValue(S = RankingKey.partition poolId matchId)
                          PoolTable.Attribute.getPoolMatchBetsByTotalSk, AttributeValue(S = decoded.RankingSortKey) ]
                )
            )
