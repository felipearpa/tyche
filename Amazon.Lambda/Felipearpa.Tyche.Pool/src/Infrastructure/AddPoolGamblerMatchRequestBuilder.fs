namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb

module AddPoolGamblerMatchRequestBuilder =

    let build (item: IDictionary<string, AttributeValue>) =
        let conditionExpression =
            $"attribute_not_exists({Key.pk}) AND attribute_not_exists({Key.sk})"

        PutItemRequest(TableName = PoolTable.name, Item = Dictionary item, ConditionExpression = conditionExpression)
