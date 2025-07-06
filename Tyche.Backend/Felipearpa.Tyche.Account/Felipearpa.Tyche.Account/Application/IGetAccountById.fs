namespace Felipearpa.Tyche.Account.Application

open Felipearpa.Tyche.Account.Domain
open Felipearpa.Type

type IGetAccountById =
    abstract ExecuteAsync: Ulid -> Result<Account option, unit> Async
