namespace Pipel.Data.DynamoDb

open System.Collections.Generic
open System.Linq
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Pipel.Core
open Pipel.Data.DynamoDb

type DbFilter = string * IDictionary<string, AttributeValue> * IDictionary<string, string> option

module Repository =

    let asyncFindWithCursorPagination<'TModel when 'TModel: not struct>
        (tableName: string)
        (filter: DbFilter option)
        (mapFunc: IDictionary<string, AttributeValue> -> 'TModel)
        (next: string option)
        (keySerializer: IKeySerializer)
        (client: IAmazonDynamoDB)
        : Async<'TModel CursorPage> =
        async {
            let request = ScanRequest(tableName)

            if filter.IsSome then
                let filterExpression, filterValues, nameExpression = filter.Value

                request.FilterExpression <- filterExpression
                request.ExpressionAttributeValues <- Dictionary(filterValues)

                if nameExpression.IsSome then
                    request.ExpressionAttributeNames <- Dictionary(nameExpression.Value)

            if next.IsSome then
                request.ExclusiveStartKey <- Dictionary(keySerializer.Deserialize(next.Value))

            let! response = client.ScanAsync(request) |> Async.AwaitTask
            let lastEvaluatedKey = response.LastEvaluatedKey

            return
                { CursorPage.Items = response.Items.Select(fun it -> mapFunc it)
                  NextToken =
                      match lastEvaluatedKey.Count with
                      | 0 -> None
                      | _ ->
                          keySerializer.Serialize(response.LastEvaluatedKey)
                          |> Some }
        }
