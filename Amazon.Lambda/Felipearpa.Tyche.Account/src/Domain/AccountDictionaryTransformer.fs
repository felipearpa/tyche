namespace Felipearpa.Tyche.Account.Domain

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open System.Runtime.CompilerServices
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Account.Infrastructure
open Felipearpa.Type

module AccountDictionaryTransformer =
    let toAccount (dictionary: IDictionary<string, AttributeValue>) : Account =
        { Account.AccountId = Ulid.newOf dictionary[AccountTable.Attribute.accountId].S
          Email = Email.newOf dictionary[AccountTable.Attribute.email].S
          ExternalAccountId = NonEmptyString.newOf dictionary[AccountTable.Attribute.externalAccountId].S }

    type Extensions =
        [<Extension>]
        static member ToAccount(this: IDictionary<string, AttributeValue>) = toAccount this
