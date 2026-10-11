namespace Felipearpa.Data.DynamoDb

open System
open System.Collections.Generic
open Amazon.DynamoDBv2.Model

/// What a query requires of one attribute of its `ExclusiveStartKey`.
type KeyAttributeRule =
    /// A string equal to the query's fixed partition or key value.
    | StringEqualTo of string
    /// A string that begins with the given prefix.
    | StringStartingWith of string
    /// Any string.
    | AnyString
    /// Any number.
    | AnyNumber

/// The exact start-key shape of one query: the union of the table's and the index's key
/// attributes, each with its type and scope. Request builders declare one per query so a
/// decoded continuation is checked before it reaches DynamoDB; no table description is read.
type StartKeyContract = Map<string, KeyAttributeRule>

module StartKeyContract =

    let private typeOf (attribute: AttributeValue) =
        ScalarKeyValue.ofAttributeValue attribute
        |> Option.map ScalarKeyValue.typeName
        |> Option.defaultValue "a non-scalar value"

    let private checkAttribute (name: string) (rule: KeyAttributeRule) (attribute: AttributeValue) =
        match rule, ScalarKeyValue.ofAttributeValue attribute with
        | StringEqualTo expected, Some(S actual) when actual = expected -> Ok()
        | StringEqualTo _, Some(S _) -> Error(KeyOutOfScope(name, "the value belongs to another partition or key"))
        | StringStartingWith prefix, Some(S actual) when actual.StartsWith(prefix, StringComparison.Ordinal) -> Ok()
        | StringStartingWith prefix, Some(S _) -> Error(KeyOutOfScope(name, $"the value does not start with '{prefix}'"))
        | AnyString, Some(S _) -> Ok()
        | AnyNumber, Some(N _) -> Ok()
        | AnyNumber, _ -> Error(WrongKeyAttributeType(name, "N", typeOf attribute))
        | _ -> Error(WrongKeyAttributeType(name, "S", typeOf attribute))

    /// Checks that a decoded key has exactly the contract's attributes, with their types and
    /// within their scope.
    let check (contract: StartKeyContract) (key: IDictionary<string, AttributeValue>) : Result<unit, InvalidContinuation> =
        let missing = contract |> Map.toSeq |> Seq.map fst |> Seq.tryFind (key.ContainsKey >> not)
        let unexpected = key.Keys |> Seq.sort |> Seq.tryFind (contract.ContainsKey >> not)

        match missing, unexpected with
        | Some name, _ -> Error(MissingKeyAttribute name)
        | None, Some name -> Error(UnexpectedKeyAttribute name)
        | None, None ->
            contract
            |> Map.toSeq
            |> Seq.map (fun (name, rule) -> checkAttribute name rule key[name])
            |> Seq.tryPick (function
                | Error error -> Some error
                | Ok() -> None)
            |> Option.map Error
            |> Option.defaultValue (Ok())

    /// Decodes `token` and checks it against `contract`, giving the start key for the query.
    /// Raises `InvalidContinuationException` when the token is unusable for this query.
    let startKey
        (contract: StartKeyContract)
        (deserialize: string -> IDictionary<string, AttributeValue>)
        (token: string)
        : Dictionary<string, AttributeValue> =
        let key = deserialize token

        match check contract key with
        | Ok() -> Dictionary key
        | Error error -> raise (InvalidContinuationException error)
