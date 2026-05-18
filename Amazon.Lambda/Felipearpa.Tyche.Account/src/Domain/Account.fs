namespace Felipearpa.Tyche.Account.Domain

open Felipearpa.Type

type Account =
    { AccountId: Ulid
      Email: Email
      Username: NonEmptyString100
      ExternalAccountId: NonEmptyString }
