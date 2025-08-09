namespace Felipearpa.Tyche.Account.Domain

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open System.Runtime.CompilerServices
open Felipearpa.Type

module AccountDictionaryTransformer =
    let toAccount (dictionary: IDictionary<string, AttributeValue>) : Account =
        { Account.AccountId = Ulid.newOf dictionary["accountId"].S
          Email = Email.newOf dictionary["email"].S
          ExternalAccountId = NonEmptyString.newOf dictionary["externalAccountId"].S }

    type Extensions =
        [<Extension>]
        static member ToAccount(this: IDictionary<string, AttributeValue>) = toAccount this
