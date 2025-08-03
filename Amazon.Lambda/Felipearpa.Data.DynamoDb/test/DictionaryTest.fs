namespace Felipearpa.Data.DynamoDb.Test

open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb.Dictionary
open FsUnitTyped
open Xunit

module DictionaryTest =

    [<Fact>]
    let ``given a existing key when its value is query then the value is returned`` () =
        let dictionary = dict [ "key", AttributeValue(S = "value") ]
        let value = dictionary |> tryGetAttributeValueOrNone "key"
        value.IsSome |> shouldEqual true
        value.Value.S |> shouldEqual "value"

    [<Fact>]
    let ``given a non existing key when its value is query then none is returned`` () =
        let dictionary = dict [ "key", AttributeValue(S = "value") ]
        let value = dictionary |> tryGetAttributeValueOrNone "non-existing-key"
        value.IsNone |> shouldEqual true
