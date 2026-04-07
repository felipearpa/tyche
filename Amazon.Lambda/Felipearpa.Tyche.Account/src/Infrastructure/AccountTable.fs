namespace Felipearpa.Tyche.Account.Infrastructure

module AccountTable =

    [<Literal>]
    let name = "Account"

    module Prefix =

        [<Literal>]
        let account = "ACCOUNT"

        [<Literal>]
        let email = "EMAIL"

    module Attribute =

        [<Literal>]
        let accountId = "accountId"

        [<Literal>]
        let email = "email"

        [<Literal>]
        let externalAccountId = "externalAccountId"

    module Index =

        [<Literal>]
        let getByEmail = "GetByEmail-index"
