module AccountDynamoDbRepositoryTest

#nowarn "3536"

open System
open System.Collections.Generic
open System.Threading.Tasks
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Account.Domain
open Felipearpa.Tyche.Account.Domain.AccountDictionaryTransformer
open Felipearpa.Tyche.Account.Infrastructure
open Felipearpa.Type
open FsUnitTyped
open Moq
open FsUnit.Xunit
open Xunit

let email = "email@felipearpa.com"
let clientMock = Mock<IAmazonDynamoDB>()
let queryResponseMock = Mock<QueryResponse>()

let accountMap =
    dict
        [ "pk", AttributeValue("ACCOUNT#01H1CMCDHH99FH8FDY36S0YH3A")
          "accountId", AttributeValue("01H1CMCDHH99FH8FDY36S0YH3A")
          "email", AttributeValue("email@felipearpa.com")
          "externalAccountId", AttributeValue("ZaGjvMlgGxbhW0yK5mYK0Lg6Wl93") ]

let accountLink =
    { AccountLink.Email = Email.newOf email
      ExternalAccountId = "ZaGjvMlgGxbhW0yK5mYK0Lg6Wl93" }

let account =
    { Account.AccountId = Ulid.newOf "01H1CMCDHH99FH8FDY36S0YH3A"
      Email = Email.newOf email
      ExternalAccountId = NonEmptyString.newOf "ZaGjvMlgGxbhW0yK5mYK0Lg6Wl93" }

let accountRepository =
    AccountDynamoDbRepository(clientMock.Object) :> IAccountRepository

let ``given an associated email`` () =
    queryResponseMock.Object.Items <- [ (accountMap |> Dictionary) ] |> List

    clientMock
        .Setup(fun client -> client.QueryAsync(It.IsAny<QueryRequest>()))
        .Returns(Task.FromResult(queryResponseMock.Object))
    |> ignore

let ``given a no associated email`` () =
    queryResponseMock.Object.Items <- [] |> List

    clientMock
        .Setup(fun client -> client.QueryAsync(It.IsAny<QueryRequest>()))
        .Returns(Task.FromResult(queryResponseMock.Object))
    |> ignore

let ``given an exception causing email`` () =
    clientMock
        .Setup(fun client -> client.QueryAsync(It.IsAny<QueryRequest>()))
        .Throws<Exception>()
    |> ignore

let ``when an account is searched by the email`` () =
    async { return! accountRepository.GetByEmailAsync(Email.newOf email) }

let ``then the associated account is returned`` (result: Result<Option<Account>, unit>) =
    match result with
    | Ok maybeAccount ->
        match maybeAccount with
        | None -> failwith "Not account queried"
        | Some queriedAccount -> queriedAccount |> shouldEqual (accountMap.ToAccount())
    | Error _ -> failwith "Error querying the account"

let ``then no account is returned`` (result: Result<Option<Account>, unit>) =
    match result with
    | Ok maybeAccount ->
        match maybeAccount with
        | None -> ()
        | Some _ -> failwith "Account gotten"
    | Error _ -> failwith "Error querying the account"

let ``then a GetAccountByEmailError is returned`` (result: Result<Option<Account>, unit>) =
    match result with
    | Ok maybeAccount ->
        match maybeAccount with
        | None -> failwith "Not account queried"
        | Some _ -> failwith "Account gotten"
    | Error _ -> ()

let ``given an associated AccountLink`` () =
    ``given an associated email`` ()

    let updateItemResponseMock = Mock<UpdateItemResponse>()

    clientMock
        .Setup(fun client -> client.UpdateItemAsync(It.IsAny<UpdateItemRequest>()))
        .Returns(Task.FromResult(updateItemResponseMock.Object))
    |> ignore

let ``when an account is linked`` () =
    async { return! accountRepository.LinkAsync accountLink }

let ``then the linked account is updated and returned`` (result: Result<Account, unit>) =
    clientMock.Verify(fun client -> client.UpdateItemAsync(It.IsAny<UpdateItemRequest>()))

    match result with
    | Ok linkedAccount -> linkedAccount |> should equal account
    | Error _ -> failwith "Result is not success"

let ``given a no associated AccountLink`` () = ``given a no associated email`` ()

let ``then a new account is returned`` (result: Result<Account, unit>) =
    match result with
    | Ok linkedAccount ->
        linkedAccount.Email |> should equal account.Email
        linkedAccount.ExternalAccountId |> should equal account.ExternalAccountId
        linkedAccount.AccountId |> should not' account.AccountId
    | Error _ -> failwith "Result is not success"

let ``given a causing exception AccountLink`` () = ``given an exception causing email`` ()

let ``then a LinkAccountError is returned`` (result: Result<Account, unit>) =
    match result with
    | Ok _ -> failwith "Account gotten"
    | Error _ -> ()

[<Fact>]
let ``given an email when an account is searched by the email then the associated account is returned`` () =
    async {
        ``given an associated email`` ()
        let! result = ``when an account is searched by the email`` ()
        ``then the associated account is returned`` result
    }

[<Fact>]
let ``given an email when an account is searched and no account associated then no account is returned`` () =
    async {
        ``given a no associated email`` ()
        let! result = ``when an account is searched by the email`` ()
        ``then no account is returned`` result
    }

[<Fact>]
let ``given an email when an account is searched and exception occurs then the error is returned`` () =
    async {
        ``given an exception causing email`` ()
        let! result = ``when an account is searched by the email`` ()
        ``then a GetAccountByEmailError is returned`` result
    }

[<Fact>]
let ``given an associated AccountLink when is linked then the linked account is updated and returned`` () =
    async {
        ``given an associated AccountLink`` ()
        let! result = ``when an account is linked`` ()
        ``then the linked account is updated and returned`` result
    }

[<Fact>]
let ``given a no associated AccountLink when is linked then a new account is returned`` () =
    async {
        ``given a no associated AccountLink`` ()
        let! result = ``when an account is linked`` ()
        ``then a new account is returned`` result
    }

[<Fact>]
let ``given an AccountLink when is linked and a exception occurs then the a LinkAccountError is returned`` () =
    async {
        ``given a causing exception AccountLink`` ()
        let! result = ``when an account is linked`` ()
        ``then a LinkAccountError is returned`` result
    }
