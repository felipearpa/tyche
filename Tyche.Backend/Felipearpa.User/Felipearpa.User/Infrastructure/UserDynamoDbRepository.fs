namespace Felipearpa.User.Infrastructure

open System
open System.Collections.Generic
open System.Linq
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.DataModel
open Amazon.DynamoDBv2.DocumentModel
open Amazon.DynamoDBv2.Model
open Felipearpa.Crypto
open Felipearpa.User.Domain
open Felipearpa.User.Type
open Microsoft.FSharp.Core

type UserDynamoDbRepository(client: IAmazonDynamoDB, hasher: IHasher) =

    [<Literal>]
    let tableName = "User"

    [<Literal>]
    let usernameIndex = "username-index"

    [<Literal>]
    let userNameText = "USERNAME"

    let context = new DynamoDBContext(client)

    let map (dictionary: IDictionary<string, AttributeValue>) =
        context.FromDocument<UserEntity>(Document.FromAttributeMap(Dictionary(dictionary)))

    let buildUserMap (user: User) =
        let userEntity = user |> UserMapper.mapToEntity

        dict
            [ "pk", AttributeValue(userEntity.Pk)
              "userId", AttributeValue(userEntity.UserId)
              "username", AttributeValue(userEntity.Username)
              "hash", AttributeValue(userEntity.Hash) ]

    let buildUserUniqueAttributesMap user =
        dict [ "pk", AttributeValue($"#{userNameText}#{user.Username |> Username.value}") ]

    let createUserInDbAsync createUserTransaction =
        async {
            try
                let! _ = client.TransactWriteItemsAsync(createUserTransaction) |> Async.AwaitTask

                return () |> Ok
            with :? AggregateException as error when (error.InnerException :? TransactionCanceledException) ->
                return Error UserCreationFailure.UserAlreadyRegistered
        }

    let buildGetUserRequest (username: Username) =
        let mutable keyConditionExpression = "#username = :username"

        let mutable defaultAttributeValues =
            dict [ ":username", AttributeValue(username |> Username.value) ]

        let mutable defaultAttributeNames = dict [ "#username", "username" ]

        let request = QueryRequest(TableName = tableName, IndexName = usernameIndex)

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

        member this.LoginAsync(username, password) =
            async {
                let request = buildGetUserRequest username

                let! response = client.QueryAsync(request) |> Async.AwaitTask

                let maybeUser =
                    (match response.Items.FirstOrDefault() |> Option.ofObj with
                     | Some item -> item |> map |> UserMapper.mapToDomain |> Some
                     | None -> None)

                return
                    match maybeUser with
                    | Some user ->
                        match hasher.Verify(password, user.Hash) with
                        | true -> user |> Ok
                        | _ -> Error LoginFailure.InvalidPassword
                    | _ -> Error LoginFailure.UserNotFound
            }
