namespace Felipearpa.Tyche.Pool.Domain

open System
open Felipearpa.Type

/// Encodes the dedicated pool-and-match ranking index keys.
///
/// The sort key orders ascending as: known totals before unavailable ones, known totals
/// descending, then gambler ID ascending. Every Int32 total maps to a fixed-width ten-digit
/// complement, so lexical order equals numeric order across the whole domain, including
/// zero and negative values:
///
/// | Total            | Sort key                          |
/// |------------------|-----------------------------------|
/// | 2147483647       | 1#K#0000000000#&lt;gamblerId&gt;  |
/// | 0                | 1#K#2147483647#&lt;gamblerId&gt;  |
/// | -2147483648      | 1#K#4294967295#&lt;gamblerId&gt;  |
/// | unavailable      | 1#U#&lt;gamblerId&gt;            |
///
/// The leading "1" is the key schema version carried by ranked cursors.
[<RequireQualifiedAccess>]
module RankingKey =

    [<Literal>]
    let schemaVersion = 1

    let private complement (total: int) = int64 Int32.MaxValue - int64 total

    let partition (poolId: Ulid) (matchId: Ulid) = $"POOL#{poolId.Value}#MATCH#{matchId.Value}"

    let sortKey (total: TotalProjection) (gamblerId: Ulid) =
        match total with
        | KnownTotal value -> $"{schemaVersion}#K#{complement value:D10}#{gamblerId.Value}"
        | UnavailableTotal -> $"{schemaVersion}#U#{gamblerId.Value}"

    /// The schema version a sort key was written with, if it is a ranking key.
    let trySchemaVersion (sortKey: string) =
        match sortKey.Split('#') with
        | [| version; _; _; _ |]
        | [| version; _; _ |] ->
            match Int32.TryParse version with
            | true, parsed -> Some parsed
            | _ -> None
        | _ -> None
