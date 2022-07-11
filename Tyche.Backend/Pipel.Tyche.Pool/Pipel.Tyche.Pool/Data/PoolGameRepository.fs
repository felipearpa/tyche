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

type IPoolGameRepository =

    abstract AsyncFind: PoolEntityPK * string option * string option -> Async<PoolGameEntity CursorPage>

type PoolGameRepository(serializer: ISerializer, client: IAmazonDynamoDB) =

    [<Literal>]
    let tableName = "Pool"

    [<Literal>]
    let poolText = "POOL"

    [<Literal>]
    let gameText = "GAME"

    let context = new DynamoDBContext(client)

    let map (dictionary: IDictionary<string, AttributeValue>) =
        context.FromDocument<PoolGameEntity>(Document.FromAttributeMap(Dictionary(dictionary)))

    interface IPoolGameRepository with

        member this.AsyncFind(poolPK, filterText, next) =
            async {
                let keyExpression =
                    "#pk = :poolPK and begins_with(#sk, :gamePK)"

                let mutable filterExpression: string option =
                    None

                let mutable attributeValues =
                    dict [ ":poolPK", AttributeValue($"#{poolText}#{poolPK.PoolId}")
                           ":gamePK", AttributeValue($"#{gameText}#") ]

                let mutable attributeNames =
                    dict [ "#pk", "pk"; "#sk", "sk" ]

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
                        None
                        keyExpression
                        filterExpression
                        attributeValues
                        (Some attributeNames)
                        false
                        map
                        next
                        (KeySerializer(serializer))
            }
