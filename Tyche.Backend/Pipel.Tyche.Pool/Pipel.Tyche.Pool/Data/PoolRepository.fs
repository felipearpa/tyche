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

type IPoolRepository =

    abstract AsyncFind:
        PoolLayoutEntityPK * string option * string option -> Async<PoolEntity CursorPage>

type PoolRepository(serializer: ISerializer, client: IAmazonDynamoDB) =

    [<Literal>]
    let tableName = "Pool"

    [<Literal>]
    let poolLayoutText = "POOLLAYOUT"

    [<Literal>]
    let poolText = "POOL"

    let context = new DynamoDBContext(client)

    let map (dictionary: IDictionary<string, AttributeValue>) =
        context.FromDocument<PoolEntity>(Document.FromAttributeMap(Dictionary(dictionary)))

    interface IPoolRepository with

        member this.AsyncFind(poolLayoutEntityPK, filterText, next) =
            async {
                let mutable defaultCondition =
                    "begins_with(#pk, :poolLayout) and begins_with(#sk, :pool) and #pk = :poolLayoutPK"

                let mutable defaultAttributeValues =
                    dict [ ":poolLayout", AttributeValue($"#{poolLayoutText}#")
                           ":pool", AttributeValue($"#{poolText}#")
                           ":poolLayoutPK", AttributeValue($"#{poolLayoutText}#{poolLayoutEntityPK.PoolLayoutId}") ]

                let mutable defaultAttributeNames =
                    dict [ "#pk", "pk"; "#sk", "sk" ]

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
                    |> asyncScan tableName (Some filter) map next (KeySerializer(serializer))
            }
