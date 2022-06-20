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

    abstract AsyncFindWithCursorPagination: PoolEntityPK * string option * string option -> Async<PoolGamblerEntity CursorPage>

type PoolGamblerRepository(serializer: ISerializer, client: IAmazonDynamoDB) =

    [<Literal>]
    let tableName = "Pool"

    [<Literal>]
    let poolText = "POOL"

    let context = new DynamoDBContext(client)

    let map (dictionary: IDictionary<string, AttributeValue>) =
        context.FromDocument<PoolGamblerEntity>(Document.FromAttributeMap(Dictionary(dictionary)))

    interface IPoolGamblerRepository with

        member this.AsyncFindWithCursorPagination(poolPK, filterText, next) =
            async {
                let mutable defaultCondition =
                    "#pk = :poolPK"

                let mutable defaultAttributeValues =
                    dict [ ":poolPK", AttributeValue($"#{poolText}#{poolPK.PoolId}") ]

                let mutable defaultAttributeNames =
                    dict [ "#pk", "pk" ]

                if filterText.IsSome then
                    defaultCondition <-
                        defaultCondition
                        + " and contains(#filter, :filter)"

                    defaultAttributeValues <-
                        defaultAttributeValues
                        |> Dict.union (dict [ ":filter", AttributeValue(filterText.Value.ToLower()) ])

                    defaultAttributeNames <-
                        defaultAttributeNames
                        |> Dict.union (dict [ "#filter", "filter" ])

                let filter =
                    (defaultCondition, defaultAttributeValues, Some defaultAttributeNames)

                return!
                    client
                    |> asyncFindWithCursorPagination tableName (Some filter) map next (KeySerializer(serializer))
            }
