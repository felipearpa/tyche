namespace Felipearpa.Tyche.PoolLayout.Infrastructure

open System

module PoolLayoutTable =

    /// The deployed table is "PoolLayout". POOL_LAYOUT_TABLE_NAME points the code at an
    /// isolated copy used by integration and load checks.
    let name =
        Environment.GetEnvironmentVariable "POOL_LAYOUT_TABLE_NAME"
        |> Option.ofObj
        |> Option.filter (String.IsNullOrWhiteSpace >> not)
        |> Option.defaultValue "PoolLayout"

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

        [<Literal>]
        let homeTeamScore = "homeTeamScore"

        [<Literal>]
        let awayTeamScore = "awayTeamScore"

        [<Literal>]
        let round = "round"

        [<Literal>]
        let groupName = "groupName"

        /// 1 for the first published result; each supported correction increments it.
        [<Literal>]
        let resultVersion = "resultVersion"

    module Index =

        [<Literal>]
        let getOpenedPoolLayout = "GetOpenedPoolLayout-index"
