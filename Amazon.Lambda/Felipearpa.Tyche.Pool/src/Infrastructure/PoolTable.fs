namespace Felipearpa.Tyche.Pool.Infrastructure

module PoolTable =

    [<Literal>]
    let name = "Pool"

    module Prefix =

        [<Literal>]
        let pool = "POOL"

        [<Literal>]
        let gambler = "GAMBLER"

        [<Literal>]
        let match' = "MATCH"

    module Attribute =

        [<Literal>]
        let poolId = "poolId"

        [<Literal>]
        let poolName = "poolName"

        [<Literal>]
        let gamblerId = "gamblerId"

        [<Literal>]
        let gamblerUsername = "gamblerUsername"

        [<Literal>]
        let status = "status"

        [<Literal>]
        let filter = "filter"

        [<Literal>]
        let score = "score"

        [<Literal>]
        let beforeScore = "beforeScore"

        [<Literal>]
        let position = "position"

        [<Literal>]
        let beforePosition = "beforePosition"

        [<Literal>]
        let poolLayoutId = "poolLayoutId"

        [<Literal>]
        let poolLayoutVersion = "poolLayoutVersion"

        [<Literal>]
        let matchId = "matchId"

        [<Literal>]
        let matchDateTime = "matchDateTime"

        [<Literal>]
        let homeTeamId = "homeTeamId"

        [<Literal>]
        let homeTeamName = "homeTeamName"

        [<Literal>]
        let awayTeamId = "awayTeamId"

        [<Literal>]
        let awayTeamName = "awayTeamName"

        [<Literal>]
        let homeTeamBet = "homeTeamBet"

        [<Literal>]
        let awayTeamBet = "awayTeamBet"

        [<Literal>]
        let homeTeamScore = "homeTeamScore"

        [<Literal>]
        let awayTeamScore = "awayTeamScore"

        [<Literal>]
        let computedDateTime = "computedDateTime"

        [<Literal>]
        let computedRequestId = "computedRequestId"

        [<Literal>]
        let getPoolGamblerScoresByMatchPk = "getPoolGamblerScoresByMatchPk"

        [<Literal>]
        let getPoolGamblerScoresByMatchSk = "getPoolGamblerScoresByMatchSk"

    module Index =

        [<Literal>]
        let pendingPoolGamblerBets = "GetPendingPoolGamblerBets-index"

        [<Literal>]
        let finishedPoolGamblerBets = "GetFinishedPoolGamblerBets-index"

        [<Literal>]
        let scoresByGambler = "GetPoolGamblerScoresByGambler-index"

        [<Literal>]
        let scoresByPool = "GetPoolGamblerScoresByPool-index"

        [<Literal>]
        let scoresByMatch = "GetPoolGamblerScoresByMatch-index"
