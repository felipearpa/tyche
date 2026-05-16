namespace Felipearpa.Tyche.Account.Application

open Felipearpa.Type

type UpdateUsernameInput =
    { AccountId: Ulid
      Username: NonEmptyString100 }
