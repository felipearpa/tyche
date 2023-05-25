namespace Felipearpa.Data.DynamoDb

open System.Collections.Generic
open System.Linq
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb

type ScanFilter = string * IDictionary<string, AttributeValue> * IDictionary<string, string> option

module Reader =

    let asyncScan<'TModel when 'TModel: not struct>
        (tableName: string)
        (scanFilter: ScanFilter option)
        (mapFunc: IDictionary<string, AttributeValue> -> 'TModel)
        (next: string option)
        (keySerializer: IKeySerializer)
        (client: IAmazonDynamoDB)
        : Async<'TModel CursorPage> =
        async {
            let request = ScanRequest(tableName)

            if scanFilter.IsSome then
                let filterExpression, filterValues, nameExpression = scanFilter.Value

                request.FilterExpression <- filterExpression
                request.ExpressionAttributeValues <- Dictionary(filterValues)

                if nameExpression.IsSome then
                    request.ExpressionAttributeNames <- Dictionary(nameExpression.Value)

            if next.IsSome then
                request.ExclusiveStartKey <- Dictionary(keySerializer.Deserialize(next.Value))

            let! response = client.ScanAsync(request) |> Async.AwaitTask

            let lastEvaluatedKey = response.LastEvaluatedKey

            return
                { CursorPage.Items = response.Items.Select(mapFunc)
                  Next =
                    match lastEvaluatedKey.Count with
                    | 0 -> None
                    | _ -> keySerializer.Serialize(response.LastEvaluatedKey) |> Some }
        }

    let asyncQuery<'TModel when 'TModel: not struct>
        (tableName: string)
        (indexName: string option)
        (keyExpression: string)
        (filterExpression: string option)
        (attributeValues: IDictionary<string, AttributeValue>)
        (attributeNames: IDictionary<string, string> option)
        (scanIndexForward: bool)
        (mapFunc: IDictionary<string, AttributeValue> -> 'TModel)
        (next: string option)
        (keySerializer: IKeySerializer)
        (client: IAmazonDynamoDB)
        : Async<'TModel CursorPage> =
        async {
            let request = QueryRequest(tableName)

            if indexName.IsSome then
                request.IndexName <- indexName.Value

            request.ScanIndexForward <- scanIndexForward

            request.KeyConditionExpression <- keyExpression
            request.ExpressionAttributeValues <- Dictionary(attributeValues)

            if attributeNames.IsSome then
                request.ExpressionAttributeNames <- Dictionary(attributeNames.Value)

            if filterExpression.IsSome then
                request.FilterExpression <- filterExpression.Value

            if next.IsSome then
                request.ExclusiveStartKey <- Dictionary(keySerializer.Deserialize(next.Value))

            let! response = client.QueryAsync(request) |> Async.AwaitTask

            let lastEvaluatedKey = response.LastEvaluatedKey

            return
                { CursorPage.Items = response.Items.Select(fun it -> mapFunc it)
                  Next =
                    match lastEvaluatedKey.Count with
                    | 0 -> None
                    | _ -> keySerializer.Serialize(response.LastEvaluatedKey) |> Some }
        }
