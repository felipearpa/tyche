namespace Felipearpa.User.Infrastructure

open System
open System.Collections.Generic
open System.Linq
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.DataModel
open Amazon.DynamoDBv2.DocumentModel
open Amazon.DynamoDBv2.Model
open Felipearpa.Type
open Felipearpa.User.Domain
open Microsoft.FSharp.Core

type UserDynamoDbRepository(client: IAmazonDynamoDB) =
    [<Literal>]
    let tableName = "Account"

    [<Literal>]
    let emailIndex = "email-index"

    [<Literal>]
    let userNameText = "EMAIL"

    let context = new DynamoDBContext(client)

    let map (dictionary: IDictionary<string, AttributeValue>) =
        context.FromDocument<UserEntity>(Document.FromAttributeMap(Dictionary(dictionary)))

    let buildUserMap (user: User) =
        let userEntity = user |> UserMapper.mapToEntity

        dict
            [ "pk", AttributeValue(userEntity.Pk)
              "accountId", AttributeValue(userEntity.AccountId)
              "email", AttributeValue(userEntity.Email) ]

    let buildUserUniqueAttributesMap user =
        dict [ "pk", AttributeValue($"{userNameText}#{user.Email |> Email.value}") ]

    let createUserInDbAsync createUserTransaction =
        async {
            try
                let! _ = client.TransactWriteItemsAsync(createUserTransaction) |> Async.AwaitTask

                return () |> Ok
            with :? AggregateException as error when (error.InnerException :? TransactionCanceledException) ->
                return Error UserCreationFailure.UserAlreadyRegistered
        }

    let buildGetUserRequest (email: Email) =
        let mutable keyConditionExpression = "#email = :email"

        let mutable defaultAttributeValues =
            dict [ ":email", AttributeValue(email |> Email.value) ]

        let mutable defaultAttributeNames = dict [ "#email", "email" ]

        let request = QueryRequest(TableName = tableName, IndexName = emailIndex)

        request.KeyConditionExpression <- keyConditionExpression
        request.ExpressionAttributeValues <- Dictionary(defaultAttributeValues)
        request.ExpressionAttributeNames <- Dictionary(defaultAttributeNames)

        request

    interface IUserRepository with

        member this.CreateAsync(user) =
            async {
                let userMap = buildUserMap user

                let userUniquePropertiesMap = buildUserUniqueAttributesMap user

                let createUserRequest = Put(TableName = tableName, Item = (userMap |> Dictionary))

                let createUserUniqueAttributesRequest =
                    Put(
                        TableName = tableName,
                        Item = (userUniquePropertiesMap |> Dictionary),
                        ConditionExpression = "attribute_not_exists(pk)"
                    )

                let requests =
                    [ TransactWriteItem(Put = createUserRequest)
                      TransactWriteItem(Put = createUserUniqueAttributesRequest) ]

                let createUserTransaction =
                    TransactWriteItemsRequest(TransactItems = (requests |> List))

                let! result = createUserInDbAsync createUserTransaction

                return result |> Result.map (fun _ -> user)
            }

        member this.LoginAsync(email) =
            async {
                let request = buildGetUserRequest email

                let! response = client.QueryAsync(request) |> Async.AwaitTask

                let maybeUser =
                    (match response.Items.FirstOrDefault() |> Option.ofObj with
                     | Some item -> item |> map |> UserMapper.mapToDomain |> Some
                     | None -> None)

                return
                    match maybeUser with
                    | Some user -> user |> Ok
                    | _ -> Error LoginFailure.UserNotFound
            }
