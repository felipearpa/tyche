namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open System.Linq
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.DataModel
open Amazon.DynamoDBv2.DocumentModel
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Pool.Domain

type PoolDynamoDbRepository(client: IAmazonDynamoDB) =

    [<Literal>]
    let tableName = "Pool"

    [<Literal>]
    let poolText = "POOL"

    let context = new DynamoDBContext(client)

    let map (dictionary: IDictionary<string, AttributeValue>) =
        context.FromDocument<PoolEntity>(Document.FromAttributeMap(Dictionary(dictionary)))

    interface IPoolRepository with

        member this.GetPoolAsync(poolId) =
            async {
                let keyConditionExpression = "#pk = :pk and #sk = :sk"

                let mutable attributeValues =
                    dict
                        [ ":pk", AttributeValue($"{poolText}#{poolId}")
                          ":sk", AttributeValue($"{poolText}#{poolId}") ]

                let mutable attributeNames = dict [ "#pk", "pk"; "#sk", "sk" ]

                let request =
                    QueryRequest(
                        TableName = tableName,
                        KeyConditionExpression = keyConditionExpression,
                        ExpressionAttributeNames = Dictionary attributeNames,
                        ExpressionAttributeValues = Dictionary attributeValues
                    )

                let! response = client.QueryAsync(request) |> Async.AwaitTask

                let nullableValuesMap = response.Items.FirstOrDefault()

                return
                    match nullableValuesMap with
                    | null -> None
                    | valuesMap -> valuesMap |> map |> PoolMapper.mapToDomain |> Some
            }
