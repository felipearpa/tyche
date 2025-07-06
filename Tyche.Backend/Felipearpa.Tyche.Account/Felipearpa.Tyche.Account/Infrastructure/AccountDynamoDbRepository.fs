namespace Felipearpa.Tyche.Account.Infrastructure

open System
open System.Collections.Generic
open System.Linq
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Microsoft.FSharp.Core
open Felipearpa.Type
open Felipearpa.Tyche.Account.Domain
open Felipearpa.Tyche.Account.Domain.AccountDictionaryTransformer
open Felipearpa.Tyche.Account.Domain.AccountEntityTransformer
open Felipearpa.Tyche.Account.Domain.AccountLinkTransformer
open Felipearpa.Tyche.Account.Domain.AccountTransformer

type AccountDynamoDbRepository(client: IAmazonDynamoDB) =
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

            let createAccountRequest = LinkRequestBuilder.build accountEntity

            let createUniqueEmailRequest = LinkRequestBuilder.buildUniqueEmail accountEntity

            let requests =
                [ TransactWriteItem(Put = createAccountRequest)
                  TransactWriteItem(Put = createUniqueEmailRequest) ]

            let createUserTransaction =
                TransactWriteItemsRequest(TransactItems = (requests |> List))

            let! response = createAccountInDbAsync createUserTransaction

            return
                match response with
                | Ok _ ->
                    { Account.Email = accountLink.Email
                      AccountId = Ulid.newOf accountEntity.AccountId
                      ExternalAccountId = NonEmptyString.newOf accountLink.ExternalAccountId }
                    |> Ok
                | Error _ -> () |> Error
        }

    let updateLink (account: Account) (accountLink: AccountLink) =
        async {
            let accountEntity = account.ToAccountEntity()

            let updateAccountLinkRequest =
                UpdateLinkRequestBuilder.build accountEntity accountLink

            return!
                async {
                    try
                        let! _ = client.UpdateItemAsync updateAccountLinkRequest |> Async.AwaitTask

                        return
                            { Account.Email = accountLink.Email
                              AccountId = Ulid.newOf accountEntity.AccountId
                              ExternalAccountId = NonEmptyString.newOf accountLink.ExternalAccountId }
                            |> Ok
                    with _ ->
                        return () |> Error
                }
        }

    interface IAccountRepository with
        member this.GetByEmailAsync(email) =
            async {
                let request = GetByEmailRequestBuilder.build (email |> Email.value)

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
                        | valuesMap -> valuesMap.ToAccountEntity().ToAccount() |> Some |> Ok
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
                            | Some account -> return! updateLink account accountLink
                            | None -> return! linkAsync accountLink
                        | Error _ -> return () |> Error
                    }
            }

        member this.GetById(id) =
            async {
                let request = GetByIdRequestBuilder.build (id |> Ulid.value)

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
                        | valuesMap -> valuesMap.ToAccountEntity().ToAccount() |> Some |> Ok
                    | Error _ -> () |> Error
            }
