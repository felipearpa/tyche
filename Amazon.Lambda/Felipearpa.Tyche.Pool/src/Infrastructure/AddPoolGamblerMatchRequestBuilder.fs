namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model

module AddPoolGamblerMatchRequestBuilder =

    [<Literal>]
    let private tableName = "Pool"

    let build (item: IDictionary<string, AttributeValue>) =
        let conditionExpression = "attribute_not_exists(pk) AND attribute_not_exists(sk)"
        PutItemRequest(TableName = tableName, Item = Dictionary item, ConditionExpression = conditionExpression)
