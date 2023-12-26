namespace Felipearpa.Account.Infrastructure

open System
open System.Collections.Generic
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.DataModel
open Amazon.DynamoDBv2.DocumentModel
open Amazon.DynamoDBv2.Model
open Microsoft.FSharp.Core
open Felipearpa.Type
open Felipearpa.Account.Domain
open Felipearpa.Account.Domain.AccountLinkTransformer

type AccountDynamoDbRepository(client: IAmazonDynamoDB) =
    [<Literal>]
    let tableName = "Account"

    [<Literal>]
    let userNameText = "EMAIL"

    let context = new DynamoDBContext(client)

    let map (dictionary: IDictionary<string, AttributeValue>) =
        context.FromDocument<AccountEntity>(Document.FromAttributeMap(Dictionary(dictionary)))

    let buildAccountMap (accountEntity: AccountEntity) =
        dict
            [ "pk", AttributeValue(accountEntity.Pk)
              "accountId", AttributeValue(accountEntity.AccountId)
              "email", AttributeValue(accountEntity.Email)
              "externalAccountId", AttributeValue(accountEntity.ExternalAccountId) ]

    let buildUniqueEmailMap (accountEntity: AccountEntity) =
        dict [ "pk", AttributeValue($"{userNameText}#{accountEntity.Email}") ]

    let createAccountInDbAsync createUserTransaction =
        async {
            try
                let! _ = client.TransactWriteItemsAsync(createUserTransaction) |> Async.AwaitTask
                return () |> Ok
            with
            | :? AggregateException as error when (error.InnerException :? TransactionCanceledException) ->
                return () |> Ok
            | _ -> return () |> Error
        }

    interface IAccountRepository with
        member this.RegisterAsync(accountLink) =
            async {
                let accountEntity = accountLink.ToAccountEntity()
                let accountMap = buildAccountMap accountEntity
                let uniqueEmailMap = buildUniqueEmailMap accountEntity

                let createAccountRequest =
                    Put(TableName = tableName, Item = (accountMap |> Dictionary))

                let createUniqueEmailRequest =
                    Put(
                        TableName = tableName,
                        Item = (uniqueEmailMap |> Dictionary),
                        ConditionExpression = "attribute_not_exists(pk)"
                    )

                let requests =
                    [ TransactWriteItem(Put = createAccountRequest)
                      TransactWriteItem(Put = createUniqueEmailRequest) ]

                let createUserTransaction =
                    TransactWriteItemsRequest(TransactItems = (requests |> List))

                let! result = createAccountInDbAsync createUserTransaction

                return
                    result
                    |> Result.map (fun _ ->
                        { Account.Email = accountLink.Email
                          AccountId = accountEntity.AccountId |> Ulid.newOf
                          ExternalAccountId = accountLink.ExternalAccountId })
            }
