namespace Felipearpa.Tyche.Account.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model

module GetPoolByIdRequestBuilder =
    [<Literal>]
    let private poolTableName = "Pool"

    [<Literal>]
    let private poolText = "POOL"

    let build (id: string) =
        let keyConditionExpression = "#pk = :pk and #sk = :sk"

        let poolKey = $"{poolText}#{id}"

        let mutable attributeValues =
            dict [ ":pk", AttributeValue(S = poolKey); ":sk", AttributeValue(S = poolKey) ]

        let mutable attributeNames = dict [ "#pk", "pk"; "#sk", "sk" ]

        QueryRequest(
            TableName = poolTableName,
            KeyConditionExpression = keyConditionExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
