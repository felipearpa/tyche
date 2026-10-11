namespace Felipearpa.Data.DynamoDb

open System.Collections.Generic
open Amazon.DynamoDBv2.Model

type IKeySerializer =

    /// Encodes a nonempty `LastEvaluatedKey`. Use `KeySerializer.next` for a key that may be
    /// absent or empty.
    abstract Serialize: IDictionary<string, AttributeValue> -> string

    /// Decodes a continuation token into an `ExclusiveStartKey`. Raises
    /// `InvalidContinuationException` when the token cannot be decoded.
    abstract Deserialize: string -> IDictionary<string, AttributeValue>

type DynamoDbKeySerializer() =

    interface IKeySerializer with

        member this.Serialize lastEvaluatedKey =
            match ContinuationKey.ofLastEvaluatedKey lastEvaluatedKey with
            | Some key -> ContinuationToken.encode key
            | None -> invalidArg (nameof lastEvaluatedKey) "An absent or empty key has no continuation token"

        member this.Deserialize token =
            match ContinuationToken.decode token with
            | Ok key -> ContinuationKey.toAttributeValues key
            | Error error -> raise (InvalidContinuationException error)

module KeySerializer =

    /// The `next` token for a page: `None` when DynamoDB returned no key or an empty key,
    /// otherwise the encoded key. Completion never depends on the page's items.
    let next (serializer: IKeySerializer) (lastEvaluatedKey: IDictionary<string, AttributeValue>) : string option =
        match lastEvaluatedKey with
        | null -> None
        | key when key.Count = 0 -> None
        | key -> Some(serializer.Serialize key)
