namespace Felipearpa.Account.Infrastructure

open System
open System.Collections.Generic
open System.Linq
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.DataModel
open Amazon.DynamoDBv2.DocumentModel
open Amazon.DynamoDBv2.Model
open Microsoft.FSharp.Core
open Felipearpa.Type
open Felipearpa.Account.Domain
open Felipearpa.Account.Domain.AccountLinkTransformer
open Felipearpa.Account.Domain.AccountEntityTransformer

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

    let linkAsync (accountLink: AccountLink) =
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
                      AccountId = Ulid.newOf accountEntity.AccountId
                      ExternalAccountId = NonEmptyString.newOf accountLink.ExternalAccountId })
        }

    interface IAccountRepository with
        member this.GetByEmailAsync(email) =
            async {
                let keyConditionExpression = "#email = :email"

                let mutable attributeValues =
                    dict [ ":email", AttributeValue(email |> Email.value) ]

                let mutable attributeNames = dict [ "#email", "email" ]

                let request =
                    QueryRequest(
                        TableName = tableName,
                        IndexName = "email-index",
                        KeyConditionExpression = keyConditionExpression,
                        ExpressionAttributeNames = Dictionary attributeNames,
                        ExpressionAttributeValues = Dictionary attributeValues
                    )

                let! queryResponse =
                    async {
                        try
                            let! response = client.QueryAsync(request) |> Async.AwaitTask
                            return response |> Ok
                        with _ ->
                            return () |> Error
                    }

                return
                    match queryResponse with
                    | Ok response ->
                        match response.Items.FirstOrDefault() with
                        | null -> None |> Ok
                        | valuesMap -> (valuesMap |> map).ToAccount() |> Some |> Ok
                    | Error _ -> () |> Error
            }

        member this.LinkAsync(accountLink) =
            async {
                let! accountResult = (this :> IAccountRepository).GetByEmailAsync(accountLink.Email)

                return!
                    async {
                        match accountResult with
                        | Ok maybeAccount ->
                            match maybeAccount with
                            | Some account -> return account |> Ok
                            | None -> return! linkAsync accountLink
                        | Error _ -> return () |> Error
                    }
            }
