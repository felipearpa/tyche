namespace Felipearpa.User.Domain

open Felipearpa.Type
open Felipearpa.User.Type

type User =
    { UserId: Ulid
      Username: Username
      Hash: string }
