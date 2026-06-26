namespace Felipearpa.Tyche.MatchScoreIngestion.Infrastructure

module MatchScoreIngestionTable =

    [<Literal>]
    let name = "MatchScoreIngestion"

    module Prefix =

        [<Literal>]
        let match' = "MATCH"

    module Attribute =

        [<Literal>]
        let externalMatchId = "externalMatchId"

        [<Literal>]
        let poolLayoutId = "poolLayoutId"

        [<Literal>]
        let matchId = "matchId"

        [<Literal>]
        let matchDateTime = "matchDateTime"

        [<Literal>]
        let status = "status"

        [<Literal>]
        let lastPolledDateTime = "lastPolledDateTime"

        [<Literal>]
        let pollCount = "pollCount"

        [<Literal>]
        let effectiveDateTime = "effectiveDateTime"

    module Status =

        [<Literal>]
        let pending = "PENDING"

        [<Literal>]
        let completed = "COMPLETED"

        [<Literal>]
        let expired = "EXPIRED"
