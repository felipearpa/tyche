namespace Felipearpa.Tyche.PoolLayout.Data

open System.Collections.Generic
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.DataModel
open Amazon.DynamoDBv2.DocumentModel
open Amazon.DynamoDBv2.Model
open Felipearpa.Core
open Felipearpa.Data.DynamoDb
open Felipearpa.Data.DynamoDb.Repository
open Felipearpa.Tyche.PoolLayout.Data

type KeySerializer(serializer: ISerializer) =

    interface IKeySerializer with

        member this.Serialize dictionary =
            serializer.Serialize(
                {| Pk = dictionary.["pk"].S
                   Sk = dictionary.["sk"].S |}
            )

        member this.Deserialize value =
            let dictionary = serializer.Deserialize<Dictionary<string, obj>>(value)

            dict [ "pk", AttributeValue(dictionary.["pk"].ToString())
                   "sk", AttributeValue(dictionary.["sk"].ToString()) ]

type IPoolLayoutRepository =

    abstract AsyncFind: string option * string option * ScanFilter option -> Async<PoolLayoutEntity CursorPage>

type PoolLayoutRepository(serializer: ISerializer, client: IAmazonDynamoDB) =

    [<Literal>]
    let tableName = "PoolLayout"

    let context = new DynamoDBContext(client)

    let map (dictionary: IDictionary<string, AttributeValue>) =
        context.FromDocument<PoolLayoutEntity>(Document.FromAttributeMap(Dictionary(dictionary)))

    interface IPoolLayoutRepository with

        member this.AsyncFind(filterText, next, filter) =
            async {
                let customCondition, customAttributeValues, customAttributeNames =
                    match filter with
                    | Some it -> it
                    | None -> ("", dict [], None)

                let mutable defaultCondition =
                    $"begins_with(#pk, :poolLayout) and begins_with(#sk, :poolLayout) {customCondition}"

                let mutable defaultAttributeValues =
                    dict [ ":poolLayout", AttributeValue("#POOLLAYOUT") ]
                    |> Dict.union customAttributeValues

                let mutable defaultAttributeNames =
                    dict [ "#pk", "pk"; "#sk", "sk" ]
                    |> Dict.union (
                        match customAttributeNames with
                        | Some it -> it
                        | None -> dict []
                    )

                if filterText.IsSome then
                    defaultCondition <- $"{defaultCondition} and contains(#filter, :filter)"

                    defaultAttributeValues <-
                        defaultAttributeValues
                        |> Dict.union (dict [ ":filter", AttributeValue(filterText.Value.ToLower()) ])

                    defaultAttributeNames <-
                        defaultAttributeNames
                        |> Dict.union (dict [ "#filter", "filter" ])

                let (filter: ScanFilter) =
                    (defaultCondition, defaultAttributeValues, Some defaultAttributeNames)

                return!
                    client
                    |> asyncScan tableName (Some filter) map next (KeySerializer(serializer))
            }
