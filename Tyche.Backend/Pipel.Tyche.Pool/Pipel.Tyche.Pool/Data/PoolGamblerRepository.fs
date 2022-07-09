namespace Pipel.Tyche.Pool.Data

open System.Collections.Generic
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.DataModel
open Amazon.DynamoDBv2.DocumentModel
open Amazon.DynamoDBv2.Model
open Pipel.Core
open Pipel.Data.DynamoDb
open Pipel.Data.DynamoDb.Repository
open Pipel.Tyche.Pool.Data

type IPoolGamblerRepository =

    abstract AsyncFindWithCursorPagination:
        PoolEntityPK * string option * string option -> Async<PoolGamblerEntity CursorPage>

type PoolGamblerRepository(serializer: ISerializer, client: IAmazonDynamoDB) =

    [<Literal>]
    let tableName = "Pool"

    [<Literal>]
    let indexName = "pk-score-index"

    [<Literal>]
    let poolText = "POOL"

    let context = new DynamoDBContext(client)

    let map (dictionary: IDictionary<string, AttributeValue>) =
        context.FromDocument<PoolGamblerEntity>(Document.FromAttributeMap(Dictionary(dictionary)))

    interface IPoolGamblerRepository with

        member this.AsyncFindWithCursorPagination(poolPK, filterText, next) =
            async {
                let keyExpression = "#pk = :poolPK"

                let mutable filterExpression: string option =
                    None

                let mutable attributeValues =
                    dict [ ":poolPK", AttributeValue($"#{poolText}#{poolPK.PoolId}") ]

                let mutable attributeNames =
                    dict [ "#pk", "pk" ]

                if filterText.IsSome then
                    filterExpression <- Some "contains(#filter, :filter)"

                    attributeValues <-
                        attributeValues
                        |> Dict.union (dict [ ":filter", AttributeValue(filterText.Value.ToLower()) ])

                    attributeNames <-
                        attributeNames
                        |> Dict.union (dict [ "#filter", "filter" ])

                return!
                    client
                    |> asyncQuery
                        tableName
                        (Some indexName)
                        keyExpression
                        filterExpression
                        attributeValues
                        (Some attributeNames)
                        false
                        map
                        next
                        (KeySerializer(serializer))
            }
