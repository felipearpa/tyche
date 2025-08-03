namespace Felipearpa.Data.DynamoDb

open System.Collections.Generic
open Amazon.DynamoDBv2.Model

module Dictionary =

    let tryGetAttributeValueOrNone (key: string) (dict: IDictionary<string, AttributeValue>) =
        match dict.TryGetValue key with
        | true, value -> Option.ofObj value
        | _ -> None
