namespace Felipearpa.Account.Domain

type IAccountRepository =
    abstract RegisterAsync: AccountLink -> Result<Account, unit> Async
