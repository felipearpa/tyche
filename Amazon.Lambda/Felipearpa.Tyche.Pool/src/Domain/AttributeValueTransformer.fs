namespace Felipearpa.Tyche.Pool.Domain

open Amazon.DynamoDBv2.Model

[<AutoOpen>]
module AttributeValueTransformer =

    let noneIfZero (maybeAttributeValue: AttributeValue option) =
        match maybeAttributeValue with
        | Some attributeValue ->
            let value = int attributeValue.N
            if value = 0 then None else Some value
        | None -> None
