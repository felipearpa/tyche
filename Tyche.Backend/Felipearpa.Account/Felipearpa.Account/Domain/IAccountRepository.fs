namespace Felipearpa.Account.Domain

open Felipearpa.Type

type IAccountRepository =
    abstract GetByEmailAsync: Email -> Result<Account Option, unit> Async
    abstract LinkAsync: AccountLink -> Result<Account, unit> Async
