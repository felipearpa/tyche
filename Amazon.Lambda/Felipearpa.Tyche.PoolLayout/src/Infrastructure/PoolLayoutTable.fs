namespace Felipearpa.Tyche.PoolLayout.Infrastructure

module PoolLayoutTable =

    [<Literal>]
    let name = "PoolLayout"

    module Prefix =

        [<Literal>]
        let poolLayout = "POOLLAYOUT"

        [<Literal>]
        let match' = "MATCH"

    module Attribute =

        [<Literal>]
        let poolLayoutId = "poolLayoutId"

        [<Literal>]
        let poolLayoutName = "poolLayoutName"

        [<Literal>]
        let poolLayoutVersion = "poolLayoutVersion"

        [<Literal>]
        let startDateTime = "startDateTime"

        [<Literal>]
        let status = "status"

        [<Literal>]
        let matchId = "matchId"

        [<Literal>]
        let homeTeamId = "homeTeamId"

        [<Literal>]
        let homeTeamName = "homeTeamName"

        [<Literal>]
        let awayTeamId = "awayTeamId"

        [<Literal>]
        let awayTeamName = "awayTeamName"

        [<Literal>]
        let matchDateTime = "matchDateTime"

    module Index =

        [<Literal>]
        let getOpenedPoolLayout = "GetOpenedPoolLayout-index"
