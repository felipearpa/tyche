namespace Felipearpa.Tyche.Account.Infrastructure

#nowarn "3536"

open System
open System.Collections.Generic
open System.Linq
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Microsoft.FSharp.Core
open Felipearpa.Type
open Felipearpa.Tyche.Account.Domain
open Felipearpa.Tyche.Account.Domain.AccountDictionaryTransformer

type AccountDynamoDbRepository(client: IAmazonDynamoDB) =
    let createAccountInDbAsync createUserTransaction =
        async {
            try
                let! _ = client.TransactWriteItemsAsync(createUserTransaction) |> Async.AwaitTask
                return Ok()
            with
            | :? AggregateException as error when (error.InnerException :? TransactionCanceledException) -> return Ok()
            | _ -> return Error()
        }

    let linkAsync (accountLink: AccountLink) =
        async {
            let newAccountId = Ulid.random ()

            let createAccountRequest = LinkRequestBuilder.build accountLink newAccountId

            let createUniqueEmailRequest = LinkRequestBuilder.buildUniqueEmail accountLink

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
                      AccountId = newAccountId
                      ExternalAccountId = NonEmptyString.newOf accountLink.ExternalAccountId }
                    |> Ok
                | Error _ -> Error()
        }

    let updateLink (account: Account) (accountLink: AccountLink) =
        async {
            let updateAccountLinkRequest = UpdateLinkRequestBuilder.build account accountLink

            return!
                async {
                    try
                        let! _ = client.UpdateItemAsync updateAccountLinkRequest |> Async.AwaitTask

                        return
                            { Account.Email = accountLink.Email
                              AccountId = account.AccountId
                              ExternalAccountId = NonEmptyString.newOf accountLink.ExternalAccountId }
                            |> Ok
                    with _ ->
                        return Error()
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
                            return Error()
                    }

                return
                    match queryResponse with
                    | Ok response ->
                        match response.Items.FirstOrDefault() with
                        | null -> None |> Ok
                        | valuesMap -> valuesMap.ToAccount() |> Some |> Ok
                    | Error _ -> Error()
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
                        | Error _ -> return Error()
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
                            return Error()
                    }

                return
                    match queryResponse with
                    | Ok response ->
                        match response.Items.FirstOrDefault() with
                        | null -> None |> Ok
                        | valuesMap -> valuesMap.ToAccount() |> Some |> Ok
                    | Error _ -> Error()
            }
