namespace Felipearpa.Tyche.Account.Domain

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open System.Runtime.CompilerServices

module AccountDictionaryTransformer =
    let toAccountEntity (dictionary: IDictionary<string, AttributeValue>) =
        { AccountEntity.Pk = dictionary["pk"].S
          AccountId = dictionary["accountId"].S
          Email = dictionary["email"].S
          ExternalAccountId = dictionary["externalAccountId"].S }

    [<Extension>]
    type Extensions =
        [<Extension>]
        static member ToAccountEntity(this: IDictionary<string, AttributeValue>) = toAccountEntity this
