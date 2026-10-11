namespace Felipearpa.Data.DynamoDb

open System
open System.Collections.Generic
open System.IO
open System.Text
open System.Text.Json
open System.Text.RegularExpressions
open Amazon.DynamoDBv2.Model

/// One scalar DynamoDB key attribute. Numbers keep DynamoDB's decimal text and are never
/// converted to a binary floating-point or decimal value.
type ScalarKeyValue =
    | S of string
    | N of string
    | B of byte array

/// Why a supplied continuation token cannot be used. Callers report every case as an
/// invalid cursor; the details are for diagnostics only.
type InvalidContinuation =
    | MalformedToken of reason: string
    | UnsupportedVersion of version: int
    | UnsupportedAttributeType of attributeName: string * typeName: string
    | InvalidAttributeValue of attributeName: string * reason: string
    /// The decoded key lacks an attribute the requested query's start key needs.
    | MissingKeyAttribute of attributeName: string
    /// The decoded key has an attribute the requested query's start key does not use.
    | UnexpectedKeyAttribute of attributeName: string
    /// An attribute has a different scalar type than the requested query's key.
    | WrongKeyAttributeType of attributeName: string * expected: string * actual: string
    /// An attribute's value lies outside the requested query's partition or key prefix.
    | KeyOutOfScope of attributeName: string * reason: string

/// Raised when a supplied continuation token cannot be decoded or does not fit the requested query.
exception InvalidContinuationException of InvalidContinuation with
    override this.Message = $"Invalid continuation token: %A{this.Data0}"

/// A complete, nonempty DynamoDB key returned as `LastEvaluatedKey`, keyed by attribute name.
type ContinuationKey = private ContinuationKey of Map<string, ScalarKeyValue>

module ScalarKeyValue =

    let private numberPattern =
        Regex(@"^-?(?<int>\d+)(\.(?<frac>\d+))?([eE][+-]?\d+)?$", RegexOptions.CultureInvariant)

    [<Literal>]
    let private maxSignificantDigits = 38

    let private significantDigits (text: string) =
        let matched = numberPattern.Match text
        let digits = (matched.Groups["int"].Value + matched.Groups["frac"].Value).Trim('0')
        digits.Length

    /// Checks that a value can be a DynamoDB key value: nonempty strings and binary values,
    /// and numbers in DynamoDB's decimal syntax with at most 38 significant digits. The
    /// magnitude range is left to DynamoDB.
    let validate (value: ScalarKeyValue) : Result<ScalarKeyValue, string> =
        match value with
        | S text when String.IsNullOrEmpty text -> Error "a string key value must not be empty"
        | B bytes when isNull bytes || bytes.Length = 0 -> Error "a binary key value must not be empty"
        | N text when isNull text || not (numberPattern.IsMatch text) -> Error "not a DynamoDB number"
        | N text when significantDigits text > maxSignificantDigits ->
            Error $"a DynamoDB number has at most {maxSignificantDigits} significant digits"
        | valid -> Ok valid

    let internal typeName (value: ScalarKeyValue) =
        match value with
        | S _ -> "S"
        | N _ -> "N"
        | B _ -> "B"

    let private hasNonScalarType (attribute: AttributeValue) =
        attribute.BOOL.HasValue
        || attribute.NULL.HasValue
        || attribute.IsMSet
        || attribute.IsLSet
        || attribute.IsSSSet
        || attribute.IsNSSet
        || attribute.IsBSSet

    let internal ofAttributeValue (attribute: AttributeValue) : ScalarKeyValue option =
        match attribute with
        | null -> None
        | attribute when hasNonScalarType attribute -> None
        | attribute ->
            match attribute.S, attribute.N, attribute.B with
            | text, null, null when not (isNull text) -> Some(S text)
            | null, number, null when not (isNull number) -> Some(N number)
            | null, null, bytes when not (isNull bytes) -> Some(B(bytes.ToArray()))
            | _ -> None

    let internal toAttributeValue (value: ScalarKeyValue) =
        match value with
        | S text -> AttributeValue(S = text)
        | N number -> AttributeValue(N = number)
        | B bytes -> AttributeValue(B = new MemoryStream(Array.copy bytes))

module ContinuationKey =

    /// Builds a key from validated attributes; fails when there are none.
    let tryCreate (attributes: Map<string, ScalarKeyValue>) : Result<ContinuationKey, InvalidContinuation> =
        let invalid =
            attributes
            |> Map.toSeq
            |> Seq.tryPick (fun (name, value) ->
                match ScalarKeyValue.validate value with
                | Error reason -> Some(InvalidAttributeValue(name, reason))
                | Ok _ -> None)

        match invalid with
        | Some error -> Error error
        | None when attributes.IsEmpty -> Error(MalformedToken "the key has no attributes")
        | None when attributes |> Map.exists (fun name _ -> String.IsNullOrEmpty name) ->
            Error(MalformedToken "an attribute name is empty")
        | None -> Ok(ContinuationKey attributes)

    let attributes (ContinuationKey attributes) = attributes

    /// Normalizes DynamoDB's `LastEvaluatedKey`: an absent or empty key means traversal is
    /// complete and yields `None`. Raises when DynamoDB returned a key that is not a valid
    /// scalar key, since that is invalid service state rather than a client error.
    let ofLastEvaluatedKey (lastEvaluatedKey: IDictionary<string, AttributeValue>) : ContinuationKey option =
        match lastEvaluatedKey with
        | null -> None
        | key when key.Count = 0 -> None
        | key ->
            let attributes =
                key
                |> Seq.map (fun entry ->
                    match ScalarKeyValue.ofAttributeValue entry.Value with
                    | Some value -> entry.Key, value
                    | None -> invalidOp $"Key attribute '{entry.Key}' is not a string, number, or binary value")
                |> Map.ofSeq

            match tryCreate attributes with
            | Ok continuationKey -> Some continuationKey
            | Error error -> invalidOp $"DynamoDB returned an unusable key: %A{error}"

    /// A new SDK dictionary for `ExclusiveStartKey`.
    let toAttributeValues (ContinuationKey attributes) : Dictionary<string, AttributeValue> =
        Dictionary(attributes |> Map.map (fun _ value -> ScalarKeyValue.toAttributeValue value))

/// Text form of a `ContinuationKey`, carried as the opaque `next` string.
///
/// Version 1 (`encode`) is `{"v":1,"key":{"<name>":{"S"|"N"|"B":"<value>"},...}}` with binary
/// values in standard base64. `decode` also reads the legacy form `{"pk":"...","sk":"..."}` that
/// earlier backends wrote, which carries only the string table keys. See
/// docs/backend-pagination.md.
module ContinuationToken =

    [<Literal>]
    let version = 1

    let private writeJson (write: Utf8JsonWriter -> unit) =
        use stream = new MemoryStream()

        do
            use writer = new Utf8JsonWriter(stream)
            write writer

        Encoding.UTF8.GetString(stream.ToArray())

    let private writeScalar (writer: Utf8JsonWriter) (name: string) (value: ScalarKeyValue) =
        writer.WriteStartObject(name)

        match value with
        | S text -> writer.WriteString("S", text)
        | N number -> writer.WriteString("N", number)
        | B bytes -> writer.WriteString("B", Convert.ToBase64String bytes)

        writer.WriteEndObject()

    /// The version 1 token: every attribute with its name, scalar type, and exact value.
    let encode (key: ContinuationKey) : string =
        writeJson (fun writer ->
            writer.WriteStartObject()
            writer.WriteNumber("v", version)
            writer.WriteStartObject("key")
            key |> ContinuationKey.attributes |> Map.iter (writeScalar writer)
            writer.WriteEndObject()
            writer.WriteEndObject())

    let private properties (element: JsonElement) = element.EnumerateObject() |> List.ofSeq

    let private duplicateName (members: JsonProperty list) =
        members
        |> List.countBy _.Name
        |> List.tryPick (fun (name, count) -> if count > 1 then Some name else None)

    let private tryString (name: string) (element: JsonElement) =
        match element.ValueKind with
        | JsonValueKind.String ->
            try
                Ok(element.GetString())
            with :? InvalidOperationException ->
                Error(InvalidAttributeValue(name, "the string is not valid text"))
        | _ -> Error(InvalidAttributeValue(name, "the value must be a JSON string"))

    let private tryBase64 (name: string) (text: string) =
        try
            Ok(Convert.FromBase64String text)
        with :? FormatException ->
            Error(InvalidAttributeValue(name, "binary values must be base64"))

    let private readScalar (name: string) (element: JsonElement) =
        match element.ValueKind with
        | JsonValueKind.Object ->
            match properties element with
            | [ typed ] ->
                match typed.Name with
                | "S" -> tryString name typed.Value |> Result.map S
                | "N" -> tryString name typed.Value |> Result.map N
                | "B" -> tryString name typed.Value |> Result.bind (tryBase64 name) |> Result.map B
                | other -> Error(UnsupportedAttributeType(name, other))
            | _ -> Error(MalformedToken $"attribute '{name}' must have exactly one type")
        | _ -> Error(MalformedToken $"attribute '{name}' must be an object")

    let private readAttributes (members: JsonProperty list) =
        members
        |> List.fold
            (fun result entry ->
                result
                |> Result.bind (fun attributes ->
                    readScalar entry.Name entry.Value
                    |> Result.map (fun value -> attributes |> Map.add entry.Name value)))
            (Ok Map.empty)

    let private tryVersion (element: JsonElement) =
        match element.ValueKind with
        | JsonValueKind.Number ->
            match element.TryGetInt32() with
            | true, value -> Some value
            | _ -> None
        | _ -> None

    let private readVersioned (versionElement: JsonElement) (keyElement: JsonElement) =
        match tryVersion versionElement with
        | None -> Error(MalformedToken "'v' must be an integer")
        | Some other when other <> version -> Error(UnsupportedVersion other)
        | Some _ when keyElement.ValueKind <> JsonValueKind.Object -> Error(MalformedToken "'key' must be an object")
        | Some _ ->
            let members = properties keyElement

            match duplicateName members with
            | Some name -> Error(MalformedToken $"attribute '{name}' appears more than once")
            | None -> readAttributes members |> Result.bind ContinuationKey.tryCreate

    let private readLegacy (members: JsonProperty list) =
        let names = members |> List.map _.Name |> List.sort

        match names with
        | [ Key.pk; Key.sk ] ->
            members
            |> List.fold
                (fun result entry ->
                    result
                    |> Result.bind (fun attributes ->
                        tryString entry.Name entry.Value
                        |> Result.map (fun text -> attributes |> Map.add entry.Name (S text))))
                (Ok Map.empty)
            |> Result.bind ContinuationKey.tryCreate
        | _ -> Error(MalformedToken "the token is neither versioned nor a legacy pk/sk token")

    let private readRoot (root: JsonElement) =
        match root.ValueKind with
        | JsonValueKind.Object ->
            let members = properties root

            match duplicateName members, members |> List.map _.Name |> List.sort with
            | Some name, _ -> Error(MalformedToken $"property '{name}' appears more than once")
            | None, [ "key"; "v" ] ->
                let find name = (members |> List.find (fun m -> m.Name = name)).Value
                readVersioned (find "v") (find "key")
            | None, names when names |> List.contains "v" || names |> List.contains "key" ->
                Error(MalformedToken "a versioned token has exactly 'v' and 'key'")
            | None, _ -> readLegacy members
        | _ -> Error(MalformedToken "the token must be a JSON object")

    /// Reads a version 1 or legacy token. Validates the envelope, version, scalar types, and
    /// values; whether the key fits a particular query is checked by that query's caller.
    let decode (token: string) : Result<ContinuationKey, InvalidContinuation> =
        match token with
        | null
        | "" -> Error(MalformedToken "the token is empty")
        | token ->
            try
                use document = JsonDocument.Parse(token)
                readRoot document.RootElement
            with :? JsonException ->
                Error(MalformedToken "the token is not JSON")
